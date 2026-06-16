package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.math.Matrix3D
import com.example.math.Vertex3D
import com.example.math.Triangle3D
import com.example.ui.viewmodel.RenderMode
import com.example.ui.theme.MinimalDark
import com.example.ui.viewmodel.HotspotConfig
import kotlin.math.sqrt

@Composable
fun ThreeDViewer(
    cameraX: Float,
    cameraY: Float,
    cameraZ: Float,
    cameraYaw: Float,
    cameraPitch: Float,
    zoomFactor: Float,
    currentRoomIndex: Int,
    wallColorIndex: Int,
    floorPatternIndex: Int,
    sceneBrightness: Float,
    isTvOn: Boolean,
    isLampOn: Boolean,
    artworkBitmap: Bitmap?,
    hotspots: List<HotspotConfig>,
    onAnglesChanged: (Float, Float) -> Unit,
    onHotspotSelected: (HotspotConfig) -> Unit,
    meshVertices: List<Vertex3D> = emptyList(),
    meshTriangles: List<Triangle3D> = emptyList(),
    renderMode: RenderMode = RenderMode.TEXTURE,
    depthScale: Float = 0.4f,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(32.dp))
            .background(MinimalDark)
            // Look around with drag touch gestures
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    // Rotate Camera Looking Angles (X movement -> Yaw, Y movement -> Pitch)
                    onAnglesChanged(
                        dragAmount.x * 0.0055f, 
                        -dragAmount.y * 0.0055f
                    )
                }
            }
    ) {
        val w = size.width
        val h = size.height

        val wFloat = w.toFloat()
        val hFloat = h.toFloat()

        // Project 3D photo mesh vertices using Matrix3D
        val projected = Array(meshVertices.size) { i ->
            val v = meshVertices[i]
            val adjustedZ = v.z * depthScale
            
            Matrix3D.project(
                Vertex3D(v.x, v.y, adjustedZ, v.r, v.g, v.b),
                cameraYaw,
                cameraPitch,
                wFloat,
                hFloat,
                zoomFactor * 0.95f,
                cameraDistance = 3.2f
            )
        }

        if (renderMode == RenderMode.POINT_CLOUD) {
            for (p in projected) {
                if (p != null) {
                    drawCircle(
                        color = Color(p.r, p.g, p.b),
                        radius = 2.0f * zoomFactor,
                        center = Offset(p.screenX, p.screenY)
                    )
                }
            }
        } else if (renderMode == RenderMode.WIREFRAME) {
            for (t in meshTriangles) {
                val p1 = projected[t.v1] ?: continue
                val p2 = projected[t.v2] ?: continue
                val p3 = projected[t.v3] ?: continue
                
                val path = Path().apply {
                    moveTo(p1.screenX, p1.screenY)
                    lineTo(p2.screenX, p2.screenY)
                    lineTo(p3.screenX, p3.screenY)
                    close()
                }
                drawPath(
                    path = path,
                    color = Color(0xFFD0BCFF).copy(alpha = 0.45f),
                    style = Stroke(0.6f * zoomFactor)
                )
            }
        } else {
            // TEXTURE or SHADED solid polygon faces
            val sortedTriangleIndices = meshTriangles.indices.sortedWith { idx1, idx2 ->
                val t1 = meshTriangles[idx1]
                val t2 = meshTriangles[idx2]
                
                val z1 = (projected[t1.v1]?.depth ?: 0f) + (projected[t1.v2]?.depth ?: 0f) + (projected[t1.v3]?.depth ?: 0f)
                val z2 = (projected[t2.v1]?.depth ?: 0f) + (projected[t2.v2]?.depth ?: 0f) + (projected[t2.v3]?.depth ?: 0f)
                
                if (z2 < z1) -1 else if (z2 > z1) 1 else 0 // descending painter's sort
            }

            val lx = 0.5f
            val ly = 1.0f
            val lz = 1.0f
            val len = sqrt(lx * lx + ly * ly + lz * lz)
            val nlx = lx / len
            val nly = ly / len
            val nlz = lz / len

            for (idx in sortedTriangleIndices) {
                val t = meshTriangles[idx]
                val p1 = projected[t.v1] ?: continue
                val p2 = projected[t.v2] ?: continue
                val p3 = projected[t.v3] ?: continue

                // Backface culling
                val area = (p2.screenX - p1.screenX) * (p3.screenY - p1.screenY) - (p2.screenY - p1.screenY) * (p3.screenX - p1.screenX)
                if (area <= 0f) continue
                
                val v1 = meshVertices[t.v1]
                val v2 = meshVertices[t.v2]
                val v3 = meshVertices[t.v3]
                
                val ax = v2.x - v1.x
                val ay = v2.y - v1.y
                val az = (v2.z - v1.z) * depthScale
                
                val bx = v3.x - v1.x
                val by = v3.y - v1.y
                val bz = (v3.z - v1.z) * depthScale
                
                var nx = ay * bz - az * by
                var ny = az * bx - ax * bz
                var nz = ax * by - ay * bx
                val clen = sqrt(nx*nx + ny*ny + nz*nz)
                
                val normalLightFactor: Float
                if (clen > 0f) {
                    nx /= clen
                    ny /= clen
                    nz /= clen
                    val dot = nx * nlx + ny * nly + nz * nlz
                    normalLightFactor = (dot * 0.45f + 0.55f).coerceIn(0.1f, 1.0f)
                } else {
                    normalLightFactor = 0.8f
                }

                val facePath = Path().apply {
                    moveTo(p1.screenX, p1.screenY)
                    lineTo(p2.screenX, p2.screenY)
                    lineTo(p3.screenX, p3.screenY)
                    close()
                }

                val color = if (renderMode == RenderMode.TEXTURE) {
                    val avgR = (v1.r + v2.r + v3.r) / 3
                    val avgG = (v1.g + v2.g + v3.g) / 3
                    val avgB = (v1.b + v2.b + v3.b) / 3
                    Color(
                        red = (avgR / 255.0f * normalLightFactor).coerceIn(0f, 1f),
                        green = (avgG / 255.0f * normalLightFactor).coerceIn(0f, 1f),
                        blue = (avgB / 255.0f * normalLightFactor).coerceIn(0f, 1f)
                    )
                } else {
                    val wallColor = when (wallColorIndex) {
                        0 -> Color(0xFFFEF7FF) // Ivory custom Paint
                        1 -> Color(0xFFE8F5E9) // Mint custom Paint
                        2 -> Color(0xFFE0E0E0) // Smoke Clay custom Paint
                        else -> Color(0xFFF3E5F5) // Lavender custom Paint
                    }
                    Color(
                        red = (wallColor.red * normalLightFactor).coerceIn(0f, 1f),
                        green = (wallColor.green * normalLightFactor).coerceIn(0f, 1f),
                        blue = (wallColor.blue * normalLightFactor).coerceIn(0f, 1f)
                    )
                }

                drawPath(path = facePath, color = color)
            }
        }
    }
}
