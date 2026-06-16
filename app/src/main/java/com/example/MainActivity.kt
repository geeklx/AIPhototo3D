package com.example

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.math.Triangle3D
import com.example.math.Vertex3D
import com.example.ui.components.ControlPanel
import com.example.ui.components.HistoryPanel
import com.example.ui.components.StereoGenerator
import com.example.ui.components.ThreeDViewer
import com.example.ui.theme.MinimalBg
import com.example.ui.theme.MinimalBorder
import com.example.ui.theme.MinimalDark
import com.example.ui.theme.MinimalGrayText
import com.example.ui.theme.MinimalLightGray
import com.example.ui.theme.MinimalPrimary
import com.example.ui.theme.MinimalSecondaryBg
import com.example.ui.theme.MinimalText
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.RenderMode
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    MainScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel()

    // State bindings
    val historicalModels by viewModel.historicalModels.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val vertices by viewModel.vertices.collectAsState()
    val triangles by viewModel.triangles.collectAsState()
    val renderMode by viewModel.renderMode.collectAsState()
    val depthStrength by viewModel.depthStrength.collectAsState()
    val autoRotate by viewModel.autoRotate.collectAsState()
    val rotationSpeed by viewModel.rotationSpeed.collectAsState()
    val cameraYaw by viewModel.cameraYaw.collectAsState()
    val cameraPitch by viewModel.cameraPitch.collectAsState()
    val zoomFactor by viewModel.zoomFactor.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()

    var activeMaterialIndex by remember { mutableStateOf(0) } // Preset Shader selection (0=Clay, 1=Bronze, 2=Neon cyber)

    // Activity result launcher for Gallery choosing
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importPhoto(context, uri)
        }
    }

    // Activity result launcher for Camera capturing (compact photo)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.onPhotoCaptured(bitmap)
        }
    }

    // Auto-orbiting continuous physics thread loop
    LaunchedEffect(autoRotate, rotationSpeed) {
        while (autoRotate) {
            viewModel.incrementRotation()
            delay(16) // Smooth ~60fps looping tick
        }
    }

    // Main layout
    Column(
        modifier = modifier
            .background(MinimalBg)
    ) {
        // ------------------ Header Toolbar ------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ViewInAr,
                    contentDescription = "Logo",
                    tint = MinimalPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Photo to 3D",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MinimalText,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "照片 3D 立体深度计算",
                        fontSize = 11.sp,
                        color = MinimalGrayText
                    )
                }
            }

            // Quick capture controls
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Photo shooting
                HeaderActionButton(
                    icon = Icons.Default.CameraAlt,
                    desc = "拍照建模",
                    testTag = "camera_button",
                    onClick = { cameraLauncher.launch(null) }
                )
                // Device selection
                HeaderActionButton(
                    icon = Icons.Default.PhotoLibrary,
                    desc = "相册导入",
                    testTag = "gallery_button",
                    onClick = { galleryLauncher.launch("image/*") }
                )
            }
        }

        // ------------------ Active Viewer Scene ------------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f) // Takes remaining available vertical space flexibly
                .padding(horizontal = 16.dp)
        ) {
            if (vertices.isEmpty()) {
                // Prompt when empty
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(32.dp))
                        .background(MinimalSecondaryBg)
                        .border(1.dp, MinimalBorder.copy(alpha = 0.5f), RoundedCornerShape(32.dp))
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = "Empty Model",
                        tint = MinimalBorder,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "还没有加载 3D 模型",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MinimalText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "请点击上方 [拍照建模] 或 [相册导入]，\n本地算法即可立即解算生成对应的立体 3D 模型！",
                        fontSize = 11.sp,
                        color = MinimalGrayText,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            } else {
                // Active Interactive 3D Canvas
                ThreeDViewer(
                    vertices = vertices,
                    triangles = triangles,
                    renderMode = renderMode,
                    depthStrength = depthStrength,
                    cameraYaw = cameraYaw,
                    cameraPitch = cameraPitch,
                    zoomFactor = zoomFactor,
                    activeMaterialIndex = activeMaterialIndex,
                    isRotating = autoRotate,
                    onAnglesChanged = { yawDelta, pitchDelta ->
                        viewModel.updateCameraAngles(yawDelta, pitchDelta)
                    },
                    modifier = Modifier.fillMaxSize().testTag("3d_canvas")
                )

                // Render detail float tag overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MinimalDark.copy(alpha = 0.8f))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "VERTICES: ${vertices.size}",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MinimalBorder
                    )
                    Text(
                        text = "TRIANGLES: ${triangles.size}",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MinimalBorder
                    )
                    Text(
                        text = "ZOOM: ${String.format("%.1fx", zoomFactor)}",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }

                // Interactive Zoom In/Out touch triggers
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ZoomButton(label = "+", onClick = { viewModel.setZoom(zoomFactor + 0.15f) })
                    ZoomButton(label = "-", onClick = { viewModel.setZoom(zoomFactor - 0.15f) })
                }
            }

            // Calculations overlay when starting a scan
            if (isGenerating) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(32.dp))
                        .background(MinimalSecondaryBg.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MinimalPrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "正在利用多维法线解算立体深度...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MinimalText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "构建Watertight多边形网格中",
                            fontSize = 10.sp,
                            color = MinimalGrayText
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ------------------ Active Scroll Context Parameters ------------------
        ControlPanel(
            renderMode = renderMode,
            onRenderModeChanged = { viewModel.setRenderMode(it) },
            depthStrength = depthStrength,
            onDepthStrengthChanged = { viewModel.setDepthStrength(it) },
            autoRotate = autoRotate,
            onAutoRotateChanged = { viewModel.setAutoRotate(it) },
            rotationSpeed = rotationSpeed,
            onRotationSpeedChanged = { viewModel.setRotationSpeed(it) },
            activeMaterialIndex = activeMaterialIndex,
            onMaterialIndexChanged = { activeMaterialIndex = it },
            onExportTriggered = { format ->
                if (vertices.isEmpty()) {
                    Toast.makeText(context, "请先生成 3D 模型再导出！", Toast.LENGTH_SHORT).show()
                } else {
                    val fileContent = viewModel.getExportedString(format)
                    shareExportFile(context, fileContent, format)
                }
            },
            onShareTriggered = {
                if (vertices.isEmpty()) {
                    Toast.makeText(context, "请先生成 3D 模型再分享！", Toast.LENGTH_SHORT).show()
                } else {
                    // 1. Generate high-fidelity Side-by-Side stereo bitmap
                    val stereoBitmap = StereoGenerator.createStereogram(
                        vertices = vertices,
                        triangles = triangles,
                        yaw = cameraYaw,
                        pitch = cameraPitch,
                        depthScale = depthStrength,
                        renderMode = renderMode,
                        materialIndex = activeMaterialIndex
                    )
                    // 2. Automatically save it to local gallery album
                    val savedUri = StereoGenerator.saveBitmapToGallery(context, stereoBitmap, "StereoView")
                    if (savedUri != null) {
                        Toast.makeText(context, "立体图已自动保存至相册！", Toast.LENGTH_SHORT).show()
                    }
                    // 3. Launch System share panel
                    shareStereoImage(context, stereoBitmap)
                }
            },
            modifier = Modifier.height(290.dp) // Maintain consistent density
        )

        // ------------------ Bottom Room History Scroll ------------------
        HistoryPanel(
            historicalModels = historicalModels,
            selectedModel = selectedModel,
            onModelSelect = { viewModel.loadModel(it) },
            onModelDelete = { viewModel.deleteModel(it) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun HeaderActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MinimalSecondaryBg)
            .border(1.dp, MinimalBorder.copy(alpha = 0.5f), CircleShape)
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = desc,
            tint = MinimalPrimary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun ZoomButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MinimalDark.copy(alpha = 0.8f))
            .border(1.dp, MinimalBorder.copy(alpha = 0.3f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MinimalBorder
        )
    }
}

// Share text format files securely via System Intent
private fun shareExportFile(context: Context, content: String, format: String) {
    try {
        val extension = format.lowercase()
        val filename = "Photo3D_Model_${System.currentTimeMillis()}.$extension"
        val file = File(context.cacheDir, filename)
        FileOutputStream(file).use { out ->
            out.write(content.toByteArray())
        }

        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "PhotoTo3D $format Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享 3D 网格模型 ($format)"))
    } catch (e: Exception) {
        Toast.makeText(context, "导出分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// Share image files via System Intent
private fun shareStereoImage(context: Context, bitmap: Bitmap) {
    try {
        val file = File(context.cacheDir, "StereoView_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }

        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "PhotoTo3D Parallel Stereo Scene")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享 3D 立体图像"))
    } catch (e: Exception) {
        Toast.makeText(context, "分享发生错误: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
