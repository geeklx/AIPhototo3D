package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MinimalAccentContainer
import com.example.ui.theme.MinimalBg
import com.example.ui.theme.MinimalBorder
import com.example.ui.theme.MinimalCardBg
import com.example.ui.theme.MinimalDark
import com.example.ui.theme.MinimalGrayText
import com.example.ui.theme.MinimalLightGray
import com.example.ui.theme.MinimalPrimary
import com.example.ui.theme.MinimalSecondaryBg
import com.example.ui.theme.MinimalText
import com.example.ui.viewmodel.RenderMode

@Composable
fun ControlPanel(
    renderMode: RenderMode,
    onRenderModeChanged: (RenderMode) -> Unit,
    depthStrength: Float,
    onDepthStrengthChanged: (Float) -> Unit,
    autoRotate: Boolean,
    onAutoRotateChanged: (Boolean) -> Unit,
    rotationSpeed: Float,
    onRotationSpeedChanged: (Float) -> Unit,
    activeMaterialIndex: Int,
    onMaterialIndexChanged: (Int) -> Unit,
    onExportTriggered: (format: String) -> Unit,
    onShareTriggered: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showFormatSelector by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MinimalBg)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // ---- 1. Rendering Mode (Segmented layout selector) ----
        Text(
            text = "3D 渲染模式 (Renderer Modes)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MinimalGrayText,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MinimalBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .background(MinimalSecondaryBg),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RenderModeOption(
                label = "贴图",
                isSelected = renderMode == RenderMode.TEXTURE,
                onClick = { onRenderModeChanged(RenderMode.TEXTURE) },
                modifier = Modifier.weight(1f)
            )
            RenderModeOption(
                label = "网格",
                isSelected = renderMode == RenderMode.WIREFRAME,
                onClick = { onRenderModeChanged(RenderMode.WIREFRAME) },
                modifier = Modifier.weight(1f)
            )
            RenderModeOption(
                label = "点云",
                isSelected = renderMode == RenderMode.POINT_CLOUD,
                onClick = { onRenderModeChanged(RenderMode.POINT_CLOUD) },
                modifier = Modifier.weight(1f)
            )
            RenderModeOption(
                label = "着色",
                isSelected = renderMode == RenderMode.SHADED,
                onClick = { onRenderModeChanged(RenderMode.SHADED) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---- 2. Lights Mat Shader Selection (visible only in Shaded Mode) ----
        AnimatedVisibility(visible = renderMode == RenderMode.SHADED) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Text(
                    text = "虚拟材质反射 (Material Presets)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MinimalGrayText,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MaterialPresetButton(
                        label = "陶泥 Clay",
                        isSelected = activeMaterialIndex == 0,
                        onClick = { onMaterialIndexChanged(0) },
                        modifier = Modifier.weight(1f)
                    )
                    MaterialPresetButton(
                        label = "青铜 Bronze",
                        isSelected = activeMaterialIndex == 1,
                        onClick = { onMaterialIndexChanged(1) },
                        modifier = Modifier.weight(1f)
                    )
                    MaterialPresetButton(
                        label = "极简紫 Purple",
                        isSelected = activeMaterialIndex == 2,
                        onClick = { onMaterialIndexChanged(2) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ---- 3. Interactive Adjustments (Sliders) ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "立体深度系数 (Height Displacement)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MinimalGrayText
            )
            Text(
                text = String.format("%.2f", depthStrength * 10f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MinimalPrimary
            )
        }
        Slider(
            value = depthStrength,
            onValueChange = onDepthStrengthChanged,
            valueRange = 0.05f..1.1f,
            modifier = Modifier.fillMaxWidth().testTag("depth_slider"),
            colors = SliderDefaults.colors(
                thumbColor = MinimalPrimary,
                activeTrackColor = MinimalPrimary,
                inactiveTrackColor = MinimalBorder.copy(alpha = 0.4f)
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // ---- 4. Auto Rotation Switch & speed ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Auto Rotate",
                    tint = MinimalGrayText,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "场景自动旋转 (Orbit Spin)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MinimalGrayText
                )
            }
            Switch(
                checked = autoRotate,
                onCheckedChange = onAutoRotateChanged,
                modifier = Modifier.testTag("rotate_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MinimalPrimary,
                    checkedTrackColor = MinimalPrimary.copy(alpha = 0.3f),
                    uncheckedThumbColor = MinimalLightGray,
                    uncheckedTrackColor = MinimalSecondaryBg
                )
            )
        }

        AnimatedVisibility(visible = autoRotate) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "物理自转速度 (Orbit Velocity)",
                        fontSize = 12.sp,
                        color = MinimalLightGray
                    )
                    Text(
                        text = String.format("%.1fx", rotationSpeed),
                        fontSize = 12.sp,
                        color = MinimalPrimary
                    )
                }
                Slider(
                    value = rotationSpeed,
                    onValueChange = onRotationSpeedChanged,
                    valueRange = 0.2f..4.0f,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MinimalPrimary,
                        activeTrackColor = MinimalPrimary,
                        inactiveTrackColor = MinimalBorder.copy(alpha = 0.4f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ---- 5. Actions: Share Stereogram and Output exporting ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Share button
            Button(
                onClick = onShareTriggered,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("share_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MinimalCardBg,
                    contentColor = MinimalDark
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share 3D Stereogram",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("分享3D立体图", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            // Expand format trigger button
            Button(
                onClick = { showFormatSelector = !showFormatSelector },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("export_trigger_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MinimalPrimary,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Unarchive,
                    contentDescription = "Export Formats",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("导出 3D 格式", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Format selector options
        AnimatedVisibility(visible = showFormatSelector) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MinimalBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .background(MinimalSecondaryBg)
                    .padding(12.dp)
            ) {
                Text(
                    text = "选择适配工业设计或 3D 打印导出的格式:",
                    fontSize = 11.sp,
                    color = MinimalLightGray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExporterFormatButton(
                        label = "Blender (.OBJ)",
                        sub = "传统网格材质制式",
                        onClick = {
                            onExportTriggered("OBJ")
                            showFormatSelector = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ExporterFormatButton(
                        label = "3D 打印 (.STL)",
                        sub = "标准工业切片制式",
                        onClick = {
                            onExportTriggered("STL")
                            showFormatSelector = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ExporterFormatButton(
                        label = "全彩点云 (.PLY)",
                        sub = "科学渲染全彩网格",
                        onClick = {
                            onExportTriggered("PLY")
                            showFormatSelector = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun RenderModeOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MinimalAccentContainer else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MinimalPrimary else MinimalLightGray
        )
    }
}

@Composable
fun MaterialPresetButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = if (isSelected) MinimalPrimary else MinimalBorder.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .background(if (isSelected) MinimalAccentContainer else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MinimalPrimary else MinimalGrayText
        )
    }
}

@Composable
fun ExporterFormatButton(
    label: String,
    sub: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MinimalAccentContainer)
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MinimalPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = sub,
            fontSize = 8.sp,
            color = MinimalGrayText,
            textAlign = TextAlign.Center
        )
    }
}

