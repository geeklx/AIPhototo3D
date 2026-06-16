package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.math.Matrix3D
import com.example.math.ProjectedVertex
import com.example.math.Triangle3D
import com.example.math.Vertex3D
import com.example.ui.theme.MinimalDark
import com.example.ui.viewmodel.RenderMode

@Composable
fun ThreeDViewer(
    vertices: List<Vertex3D>,
    triangles: List<Triangle3D>,
    renderMode: RenderMode,
    depthStrength: Float,
    cameraYaw: Float,
    cameraPitch: Float,
    zoomFactor: Float,
    activeMaterialIndex: Int, // 0 = Clay, 1 = Bronze, 2 = Cyber Neon
    isRotating: Boolean,
    onAnglesChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // If we have no vertices, display a sleek blank terminal grid
    if (vertices.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(32.dp))
                .background(MinimalDark)
        )
        return
    }

    // Material choices for Light Shaded mode
    val materialBaseColor = when (activeMaterialIndex) {
        0 -> Color(0xFFEADDFF) // M3 Lavender Clay
        1 -> Color(0xFFCD7F32) // Bright Bronze
        else -> Color(0xFF6750A4) // PolyCraft Signature Purple
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(32.dp))
            .background(MinimalDark)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    // Map drag amounts to angle deltas (X -> Yaw, Y -> Pitch)
                    onAnglesChanged(
                        dragAmount.x * 0.007f, // Yaw Rotation Speed
                        -dragAmount.y * 0.007f // Pitch Rotation Speed
                    )
                }
            }
    ) {
        val width = size.width
        val height = size.height

        // 1. Project all 3D vertices into 2D space
        val projected = Array(vertices.size) { i ->
            Matrix3D.project(
                vertex = vertices[i],
                yawRad = cameraYaw,
                pitchRad = cameraPitch,
                width = width,
                height = height,
                zoom = zoomFactor
            )
        }

        // 2. Render based on active mode
        when (renderMode) {
            RenderMode.POINT_CLOUD -> {
                // Draw glowing points directly
                for (p in projected) {
                    val color = Color(p.r, p.g, p.b)
                    drawCircle(
                        color = color,
                        radius = 2.5f * zoomFactor,
                        center = Offset(p.screenX, p.screenY)
                    )
                }
            }

            RenderMode.WIREFRAME -> {
                // Draw mesh lines with color mapping
                val strokeWidth = 1f
                val drawnEdges = HashSet<Long>()

                for (f in triangles) {
                    val p1 = projected[f.v1]
                    val p2 = projected[f.v2]
                    val p3 = projected[f.v3]

                    fun drawEdge(i1: Int, i2: Int, startX: Float, startY: Float, endX: Float, endY: Float, r: Int, g: Int, b: Int) {
                        val key = if (i1 < i2) (i1.toLong() shl 32) or i2.toLong() else (i2.toLong() shl 32) or i1.toLong()
                        if (!drawnEdges.contains(key)) {
                            drawnEdges.add(key)
                            drawLine(
                                color = Color(r, g, b, 180),
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = strokeWidth
                            )
                        }
                    }

                    drawEdge(f.v1, f.v2, p1.screenX, p1.screenY, p2.screenX, p2.screenY, p1.r, p1.g, p1.b)
                    drawEdge(f.v2, f.v3, p2.screenX, p2.screenY, p3.screenX, p3.screenY, p2.r, p2.g, p2.b)
                    drawEdge(f.v3, f.v1, p3.screenX, p3.screenY, p1.screenX, p1.screenY, p3.r, p3.g, p3.b)
                }
            }

            RenderMode.TEXTURE, RenderMode.SHADED -> {
                // For solid polygon models, calculate face depth and sort triangles to prevent overlap artifacting (Painter's Algorithm)
                val sortedTriangles = triangles.map { t ->
                    val d = (projected[t.v1].depth + projected[t.v2].depth + projected[t.v3].depth) / 3f
                    t.copy(depth = d)
                }.sortedBy { it.depth }

                for (f in sortedTriangles) {
                    val p1 = projected[f.v1]
                    val p2 = projected[f.v2]
                    val p3 = projected[f.v3]

                    val srcV1 = vertices[f.v1]
                    val srcV2 = vertices[f.v2]
                    val srcV3 = vertices[f.v3]

                    // Calculate Lambertian light factor
                    val lightFactor = Matrix3D.calculateLighting(srcV1, srcV2, srcV3)

                    // Compute flat color for the polygon face
                    val faceColor = if (renderMode == RenderMode.TEXTURE) {
                        val avgR = ((p1.r + p2.r + p3.r) / 3f * lightFactor).toInt().coerceIn(0, 255)
                        val avgG = ((p1.g + p2.g + p3.g) / 3f * lightFactor).toInt().coerceIn(0, 255)
                        val avgB = ((p1.b + p2.b + p3.b) / 3f * lightFactor).toInt().coerceIn(0, 255)
                        Color(avgR, avgG, avgB)
                    } else {
                        // Apply Light Shaded Material mode
                        val shadingR = (materialBaseColor.red * 255f * lightFactor).toInt().coerceIn(0, 255)
                        val shadingG = (materialBaseColor.green * 255f * lightFactor).toInt().coerceIn(0, 255)
                        val shadingB = (materialBaseColor.blue * 255f * lightFactor).toInt().coerceIn(0, 255)
                        Color(shadingR, shadingG, shadingB)
                    }

                    // Draw filled Path representation of the triangle
                    val path = Path().apply {
                        moveTo(p1.screenX, p1.screenY)
                        lineTo(p2.screenX, p2.screenY)
                        lineTo(p3.screenX, p3.screenY)
                        close()
                    }

                    drawPath(
                        path = path,
                        color = faceColor
                    )
                }
            }
        }
    }
}

