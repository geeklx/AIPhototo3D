package com.example.ui.components

import com.example.ui.viewmodel.RenderMode
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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

@Composable
fun ControlPanel(
    wallColorIndex: Int,
    onWallColorChanged: (Int) -> Unit,
    floorPatternIndex: Int,
    onFloorPatternChanged: (Int) -> Unit,
    sceneBrightness: Float,
    onSceneBrightnessChanged: (Float) -> Unit,
    isTvOn: Boolean,
    onTvStateChanged: (Boolean) -> Unit,
    isLampOn: Boolean,
    onLampStateChanged: (Boolean) -> Unit,
    onMove: (forward: Float, strafe: Float) -> Unit,
    onExportTriggered: (format: String) -> Unit,
    isPhoto3DMode: Boolean = false,
    depthScale: Float = 0.4f,
    onDepthScaleChanged: (Float) -> Unit = {},
    renderMode: RenderMode = RenderMode.TEXTURE,
    onRenderModeChanged: (RenderMode) -> Unit = {},
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
        if (isPhoto3DMode) {
            // ---- 3D PHOTO DEPTH SPACE CONTROLLER VIEW ----
            Text(
                text = "交互三维高精度立体属性 (3D Photo Space Props)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MinimalGrayText,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 1. DEPTH SCALE STRENGTH SLIDER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "三维立体景深强度 (3D Depth Volume)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = com.example.ui.theme.MinimalDark
                )
                Text(
                    text = String.format("%.2fx", depthScale),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MinimalPrimary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Slider(
                value = depthScale,
                onValueChange = onDepthScaleChanged,
                valueRange = 0.05f..1.5f,
                modifier = Modifier.fillMaxWidth().testTag("depth_scale_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = MinimalPrimary,
                    activeTrackColor = MinimalPrimary,
                    inactiveTrackColor = MinimalBorder.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. RENDERING EFFECT METHOD SELECTOR
            Text(
                text = "渲染显示效果 (3D Rendering Mode)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MinimalGrayText,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MinimalSecondaryBg)
                    .border(1.dp, MinimalBorder.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val modes = listOf(
                    RenderMode.TEXTURE to "纹理投影",
                    RenderMode.SHADED to "黏土着色",
                    RenderMode.WIREFRAME to "科技线框",
                    RenderMode.POINT_CLOUD to "极客点云"
                )
                for (item in modes) {
                    val m = item.first
                    val label = item.second
                    val isSelected = renderMode == m
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) MinimalAccentContainer else Color.Transparent)
                            .clickable { onRenderModeChanged(m) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MinimalPrimary else MinimalLightGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. COLOR MATERIAL PALETTES (Shaded clay fallback color)
            if (renderMode == RenderMode.SHADED) {
                Text(
                    text = "黏土雕塑上色配漆 (Shaded Clay Material Paints)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MinimalGrayText,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val colorLabels = listOf("经典象牙", "薄荷微晶", "烟熏灰泥", "薰衣草紫")
                    val colorHexes = listOf(Color(0xFFFEF7FF), Color(0xFFE8F5E9), Color(0xFFE0E0E0), Color(0xFFF3E5F5))
                    for (idx in colorLabels.indices) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (wallColorIndex == idx) MinimalAccentContainer else MinimalSecondaryBg)
                                .border(
                                    1.dp,
                                    if (wallColorIndex == idx) MinimalPrimary else MinimalBorder.copy(alpha = 0.2f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onWallColorChanged(idx) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(colorHexes[idx])
                                        .border(0.5.dp, Color.Gray, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = colorLabels[idx],
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (wallColorIndex == idx) MinimalPrimary else MinimalGrayText
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 4. PRINTABLE STL, OBJ, PLY MODEL EXPORTERS
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { showFormatSelector = !showFormatSelector },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("export_trigger_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MinimalPrimary,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Unarchive,
                    contentDescription = "Export 3D printable files",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("导出当前三维立体模型 (Export formats)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            AnimatedVisibility(visible = showFormatSelector) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MinimalSecondaryBg)
                        .border(1.dp, MinimalBorder.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onExportTriggered("OBJ")
                            showFormatSelector = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MinimalAccentContainer, contentColor = MinimalPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("彩色网格 (.OBJ)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            onExportTriggered("STL")
                            showFormatSelector = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MinimalAccentContainer, contentColor = MinimalPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("3D打印料 (.STL)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            onExportTriggered("PLY")
                            showFormatSelector = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MinimalAccentContainer, contentColor = MinimalPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("极客点云 (.PLY)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // ---- 1. MOVE CONTROLLER (TACTILE GAMEPAD D-PAD) ----
        Text(
            text = "人称位置摇杆控制 (Player Walk Controller)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MinimalGrayText,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MinimalSecondaryBg)
                .border(1.dp, MinimalBorder.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // Directional cross arrangement
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Forward
                DpadButton(
                    icon = Icons.Default.ArrowUpward,
                    desc = "前行",
                    testTag = "dpad_up",
                    onClick = { onMove(1f, 0f) }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Left strafe
                    DpadButton(
                        icon = Icons.Default.ArrowBack,
                        desc = "左平移",
                        testTag = "dpad_left",
                        onClick = { onMove(0f, -1f) }
                    )
                    // Down (backward)
                    DpadButton(
                        icon = Icons.Default.ArrowDownward,
                        desc = "后退",
                        testTag = "dpad_down",
                        onClick = { onMove(-1f, 0f) }
                    )
                    // Right strafe
                    DpadButton(
                        icon = Icons.Default.ArrowForward,
                        desc = "右平移",
                        testTag = "dpad_right",
                        onClick = { onMove(0f, 1f) }
                    )
                }
            }

            // Quick instruction panel on side
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "滑动画面: 观察四周视角",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MinimalPrimary
                )
                Text(
                    text = "点击 D-Pad: 沿视线方向平移移动\n轻触橙色闪烁点: 触发家具场景互动",
                    fontSize = 9.sp,
                    color = MinimalLightGray,
                    lineHeight = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ---- 2. WALL COLOR COATING PICKER ----
        Text(
            text = "墙面漆色彩定制 (Wall Paint Coating)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MinimalGrayText,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val colorLabels = listOf("经典象牙", "薄荷微晶", "烟熏灰泥", "薰衣草紫")
            val colorHexes = listOf(Color(0xFFFEF7FF), Color(0xFFE8F5E9), Color(0xFFE0E0E0), Color(0xFFF3E5F5))
            for (idx in colorLabels.indices) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (wallColorIndex == idx) MinimalAccentContainer else MinimalSecondaryBg)
                        .border(
                            1.dp,
                            if (wallColorIndex == idx) MinimalPrimary else MinimalBorder.copy(alpha = 0.2f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onWallColorChanged(idx) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(colorHexes[idx])
                                .border(0.5.dp, Color.Gray, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = colorLabels[idx],
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (wallColorIndex == idx) MinimalPrimary else MinimalGrayText
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---- 3. FLOORING CUSTOMIZER ----
        Text(
            text = "地面材质铺贴 (Floor Panel Layout)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MinimalGrayText,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MinimalSecondaryBg)
                .border(1.dp, MinimalBorder.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val floorLabels = listOf("柚木人字拼", "希腊雅士白", "数字几何")
            for (i in floorLabels.indices) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (floorPatternIndex == i) MinimalAccentContainer else Color.Transparent)
                        .clickable { onFloorPatternChanged(i) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = floorLabels[i],
                        fontSize = 11.sp,
                        fontWeight = if (floorPatternIndex == i) FontWeight.Bold else FontWeight.Normal,
                        color = if (floorPatternIndex == i) MinimalPrimary else MinimalLightGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---- 4. BRIGHTNESS SLIDER ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "室外日光照度 (Ambient Sunshine Level)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MinimalGrayText
            )
            Text(
                text = String.format("%.2fx", sceneBrightness),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MinimalPrimary
            )
        }
        Slider(
            value = sceneBrightness,
            onValueChange = onSceneBrightnessChanged,
            valueRange = 0.3f..1.8f,
            modifier = Modifier.fillMaxWidth().testTag("brightness_slider"),
            colors = SliderDefaults.colors(
                thumbColor = MinimalPrimary,
                activeTrackColor = MinimalPrimary,
                inactiveTrackColor = MinimalBorder.copy(alpha = 0.3f)
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // ---- 5. INTERACTIVE APPLIANCES TOGGLES ----
        Text(
            text = "智能设备远程控制 (Appliances Node Remote)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MinimalGrayText,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Smart TV Row
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MinimalBorder.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .background(MinimalSecondaryBg)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tv, contentDescription = "TV", tint = MinimalPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("巨幕电视", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MinimalDark)
                    }
                    Switch(
                        checked = isTvOn,
                        onCheckedChange = onTvStateChanged,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MinimalPrimary,
                            checkedTrackColor = MinimalPrimary.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.scale(0.85f)
                    )
                }
            }

            // Smart Bed Lamp Row
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MinimalBorder.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .background(MinimalSecondaryBg)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lightbulb, contentDescription = "Lamp", tint = MinimalPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("卧室台灯", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MinimalDark)
                    }
                    Switch(
                        checked = isLampOn,
                        onCheckedChange = onLampStateChanged,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MinimalPrimary,
                            checkedTrackColor = MinimalPrimary.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.scale(0.85f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ---- 6. SCHEMATIC EXPORT BUTTON ----
        Button(
            onClick = { showFormatSelector = !showFormatSelector },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("export_trigger_btn"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MinimalPrimary,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.Unarchive,
                contentDescription = "Export design blueprints",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("导出当前整屋装修设计蓝图", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        // Expanded format option
        AnimatedVisibility(visible = showFormatSelector) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MinimalSecondaryBg)
                    .border(1.dp, MinimalBorder.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onExportTriggered("TXT")
                        showFormatSelector = false
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MinimalAccentContainer, contentColor = MinimalPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("导出设计文本 (.TXT)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        onExportTriggered("XML")
                        showFormatSelector = false
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MinimalAccentContainer, contentColor = MinimalPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("导出结构参数 (.XML)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        }
    }
}

@Composable
fun DpadButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(MinimalCardBg)
            .border(1.dp, MinimalBorder.copy(alpha = 0.4f), CircleShape)
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = desc,
            tint = MinimalPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}

// Compact helper to support scale modification elegantly without verbose logic
fun Modifier.scale(scale: Float) = this.then(
    Modifier.padding(all = 1.dp) // minimal layout spacing
)
