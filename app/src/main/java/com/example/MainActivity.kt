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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.components.ControlPanel
import com.example.ui.components.HistoryPanel
import com.example.ui.components.ThreeDViewer
import com.example.ui.theme.MinimalBg
import com.example.ui.theme.MinimalBorder
import com.example.ui.theme.MinimalCardBg
import com.example.ui.theme.MinimalDark
import com.example.ui.theme.MinimalGrayText
import com.example.ui.theme.MinimalLightGray
import com.example.ui.theme.MinimalPrimary
import com.example.ui.theme.MinimalSecondaryBg
import com.example.ui.theme.MinimalText
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
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

    // Collect first person variables from ViewModel StateFlows
    val cameraX by viewModel.cameraX.collectAsState()
    val cameraY by viewModel.cameraY.collectAsState()
    val cameraZ by viewModel.cameraZ.collectAsState()
    val cameraYaw by viewModel.cameraYaw.collectAsState()
    val cameraPitch by viewModel.cameraPitch.collectAsState()
    val zoomFactor by viewModel.zoomFactor.collectAsState()

    val currentRoomIndex by viewModel.currentRoomIndex.collectAsState()
    val wallColorIndex by viewModel.wallColorIndex.collectAsState()
    val floorPatternIndex by viewModel.floorPatternIndex.collectAsState()
    val sceneBrightness by viewModel.sceneBrightness.collectAsState()
    val isTvOn by viewModel.isTvOn.collectAsState()
    val isLampOn by viewModel.isLampOn.collectAsState()
    val activeHotspotId by viewModel.activeHotspotId.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val artworkBitmap by viewModel.artworkBitmap.collectAsState()
    val isTransitioning by viewModel.isTransitioning.collectAsState()
    val historicalModels by viewModel.historicalModels.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()

    val isFullscreen by viewModel.isFullscreen.collectAsState()
    val depthScale by viewModel.depthScale.collectAsState()
    val renderMode by viewModel.renderMode.collectAsState()
    val meshVertices by viewModel.meshVertices.collectAsState()
    val meshTriangles by viewModel.meshTriangles.collectAsState()

    val currentRoomHotspots = viewModel.roomHotspots[currentRoomIndex] ?: emptyList()
    val activeHotspot = currentRoomHotspots.find { it.id == activeHotspotId }

    // Activity launchers for custom artwork wallpaper loading
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importPhoto(context, uri)
            Toast.makeText(context, "照片导入成功！已置于书房壁挂相屏里！", Toast.LENGTH_LONG).show()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.onPhotoCaptured(bitmap)
            Toast.makeText(context, "自拍照片导入成功！已放置到书房电子画框！", Toast.LENGTH_LONG).show()
        }
    }

    // Full screen rotation LaunchedEffect
    androidx.compose.runtime.LaunchedEffect(isFullscreen) {
        if (isFullscreen) {
            while (true) {
                // Periodically rotate camera yaw slightly to spin the 3D space automatically
                viewModel.updateCameraAngles(0.015f, 0f)
                kotlinx.coroutines.delay(16)
            }
        }
    }

    if (isFullscreen) {
        // FULLSCREEN ROTATION 3D PORT
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            ThreeDViewer(
                cameraX = cameraX,
                cameraY = cameraY,
                cameraZ = cameraZ,
                cameraYaw = cameraYaw,
                cameraPitch = cameraPitch,
                zoomFactor = zoomFactor,
                currentRoomIndex = currentRoomIndex,
                wallColorIndex = wallColorIndex,
                floorPatternIndex = floorPatternIndex,
                sceneBrightness = sceneBrightness,
                isTvOn = isTvOn,
                isLampOn = isLampOn,
                artworkBitmap = artworkBitmap,
                hotspots = currentRoomHotspots,
                onAnglesChanged = { yawDelta, pitchDelta ->
                    viewModel.updateCameraAngles(yawDelta, pitchDelta)
                },
                onHotspotSelected = { hotspot ->
                    viewModel.setHotspotId(hotspot.id)
                },
                meshVertices = meshVertices,
                meshTriangles = meshTriangles,
                renderMode = renderMode,
                depthScale = depthScale,
                modifier = Modifier.fillMaxSize()
            )

            // EXIT FULL SCREEN BUTTON
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, Color.White, CircleShape)
                    .clickable { viewModel.setFullscreen(false) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Exit fullscreen rotation",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // HUD labels in fullscreen mode
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(12.dp)
            ) {
                Text("三维立体空间・全屏旋转巡航中", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("拖拽画面可改变视角・点击右上角 [X] 回到工作区", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
            }
        }
    } else {
        Column(
            modifier = modifier
                .background(MinimalBg)
        ) {
        // ------------------ Header Toolbar & Action Pickers ------------------
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
                        text = "PolyCraft 3D",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MinimalText,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "三维相片立体空间重建与交互系统",
                        fontSize = 10.sp,
                        color = MinimalGrayText
                    )
                }
            }

            // Photo capturing & importing buttons that feed into design frames
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderActionButton(
                    icon = Icons.Default.CameraAlt,
                    desc = "拍照建模",
                    testTag = "camera_button",
                    onClick = { cameraLauncher.launch(null) }
                )
                HeaderActionButton(
                    icon = Icons.Default.PhotoLibrary,
                    desc = "相册导入",
                    testTag = "gallery_button",
                    onClick = { galleryLauncher.launch("image/*") }
                )
            }
        }

        // ------------------ Active 1st Person Viewer Scene viewport ------------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f)
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            // Render 3D CAD Blueprint First Person Player Engine
            ThreeDViewer(
                cameraX = cameraX,
                cameraY = cameraY,
                cameraZ = cameraZ,
                cameraYaw = cameraYaw,
                cameraPitch = cameraPitch,
                zoomFactor = zoomFactor,
                currentRoomIndex = currentRoomIndex,
                wallColorIndex = wallColorIndex,
                floorPatternIndex = floorPatternIndex,
                sceneBrightness = sceneBrightness,
                isTvOn = isTvOn,
                isLampOn = isLampOn,
                artworkBitmap = artworkBitmap,
                hotspots = currentRoomHotspots,
                onAnglesChanged = { yawDelta, pitchDelta ->
                    viewModel.updateCameraAngles(yawDelta, pitchDelta)
                },
                onHotspotSelected = { hotspot ->
                    viewModel.setHotspotId(hotspot.id)
                },
                meshVertices = meshVertices,
                meshTriangles = meshTriangles,
                renderMode = renderMode,
                depthScale = depthScale,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("3d_canvas")
            )

            // Overlaid Compass & Space Coordinates Indicator info
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MinimalDark.copy(alpha = 0.8f))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "【三维相片立体重建空间】",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MinimalBorder
                )
                Text(
                    text = "PITCH: ${String.format("%.2f", cameraPitch)} rad",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = "YAW: ${String.format("%.2f", cameraYaw)} rad",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            // Display Zoom in/out touch controllers
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MinimalPrimary)
                        .clickable { viewModel.setFullscreen(true) }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("全屏自动旋转 🔄", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                ZoomButton(label = "+", onClick = { viewModel.setZoom(zoomFactor + 0.15f) })
                ZoomButton(label = "-", onClick = { viewModel.setZoom(zoomFactor - 0.15f) })
            }

            // Automated smooth flight corridor tour transition alert card
            if (isTransitioning) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MinimalDark.copy(alpha = 0.85f))
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = MinimalBorder,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "拟真视角正在平稳飞行过渡中...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Calculations overlay indicator (photo height decoding on upload)
            if (isGenerating) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(32.dp))
                        .background(MinimalSecondaryBg.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MinimalPrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "正在利用多维解析提取定制画幅...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MinimalText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "自绘相屏已完成加载就绪",
                            fontSize = 10.sp,
                            color = MinimalGrayText
                        )
                    }
                }
            }

            // ---- 7. ACTIVE HOTSPOT SPEC / PROPERTY INTERACTIVITIES INTERFACING ----
            if (activeHotspot != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(0.85f)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MinimalDark.copy(alpha = 0.92f))
                        .border(1.dp, MinimalBorder.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Sensors,
                                    contentDescription = "Active sensors node",
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = activeHotspot.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .clickable { viewModel.setHotspotId(null) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "关闭",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = activeHotspot.description,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Contextual actions
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (activeHotspot.id == "living_tv") {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MinimalPrimary)
                                        .clickable { viewModel.setTvState(!isTvOn) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (isTvOn) "遥控关闭电视" else "一键开启高亮屏幕",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            } else if (activeHotspot.id == "bedroom_lamp") {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MinimalPrimary)
                                        .clickable { viewModel.setLampState(!isLampOn) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (isLampOn) "熄灭床旁暖夜灯" else "点亮智能触控台灯",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .clickable { viewModel.setHotspotId(null) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "已阅关闭详情",
                                    fontSize = 9.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // ------------------ Active Scrollable Control Customizations Deck ------------------
        ControlPanel(
            wallColorIndex = wallColorIndex,
            onWallColorChanged = { viewModel.setWallColor(it) },
            floorPatternIndex = floorPatternIndex,
            onFloorPatternChanged = { viewModel.setFloorPattern(it) },
            sceneBrightness = sceneBrightness,
            onSceneBrightnessChanged = { viewModel.setSceneBrightness(it) },
            isTvOn = isTvOn,
            onTvStateChanged = { viewModel.setTvState(it) },
            isLampOn = isLampOn,
            onLampStateChanged = { viewModel.setLampState(it) },
            onMove = { f, s -> viewModel.movePlayer(f, s) },
            onExportTriggered = { format ->
                val fileContent = viewModel.getExportedString(format)
                shareExportFile(context, fileContent, format)
            },
            isPhoto3DMode = true,
            depthScale = depthScale,
            onDepthScaleChanged = { viewModel.setDepthScale(it) },
            renderMode = renderMode,
            onRenderModeChanged = { viewModel.setRenderMode(it) },
            modifier = Modifier.weight(0.85f)
        )

        // ------------------ HISTORICAL CABINET CUSTOM DESIGN WALLPAPER RECORDS ------------------
        HistoryPanel(
            historicalModels = historicalModels,
            selectedModel = selectedModel,
            onModelSelect = { viewModel.loadModel(it) },
            onModelDelete = { viewModel.deleteModel(it) },
            modifier = Modifier.fillMaxWidth()
        )
    }
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

// Shares TXT structure configurations securely with systemic provider
private fun shareExportFile(context: Context, content: String, format: String) {
    try {
        val extension = format.lowercase()
        val filename = "PolyCraft_Design_${System.currentTimeMillis()}.$extension"
        val file = File(context.cacheDir, filename)
        FileOutputStream(file).use { out ->
            out.write(content.toByteArray())
        }

        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "PolyCraft Design Parameter Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享户型装修蓝图 ($format)"))
    } catch (e: Exception) {
        Toast.makeText(context, "导出分享发生错误: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
