package com.example.ui.components

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.math.Matrix3D
import com.example.math.ProjectedVertex
import com.example.math.Triangle3D
import com.example.math.Vertex3D
import com.example.ui.viewmodel.RenderMode
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.sin

object StereoGenerator {

    // Generates a beautiful parallel side-by-side 3D stereogram in an off-screen bitmap
    fun createStereogram(
        vertices: List<Vertex3D>,
        triangles: List<Triangle3D>,
        yaw: Float,
        pitch: Float,
        depthScale: Float,
        renderMode: RenderMode,
        materialIndex: Int
    ): Bitmap {
        val width = 1200
        val height = 720
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Slate black background in MinimalDark hue
        val bgPaint = Paint().apply {
            color = AndroidColor.parseColor("#211F26")
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Draw lavender grid line vectors to establish structural depth
        val gridPaint = Paint().apply {
            color = AndroidColor.parseColor("#34303D")
            strokeWidth = 1f
        }
        for (i in 0..width step 40) {
            canvas.drawLine(i.toFloat(), 0f, i.toFloat(), height.toFloat(), gridPaint)
        }
        for (j in 0..height step 40) {
            canvas.drawLine(0f, j.toFloat(), width.toFloat(), j.toFloat(), gridPaint)
        }

        val leftCenterX = 300f
        val rightCenterX = 900f
        val centerY = 360f

        // Binocular parallax/disparity creates a real physical depth perception when cross-eyed or side-by-side viewing
        val parallax = 0.055f 
        val leftYaw = yaw - parallax
        val rightYaw = yaw + parallax

        fun drawIndividualEye(centerX: Float, eyeYaw: Float) {
            val viewWidth = 550f
            val viewHeight = 550f

            // Project 3D coordinate list
            val projected = Array(vertices.size) { i ->
                val v = vertices[i]
                // Y (Yaw) Rotation
                val cy = cos(eyeYaw)
                val sy = sin(eyeYaw)
                val x1 = v.x * cy - v.z * sy
                val z1 = v.x * sy + v.z * cy
                val y1 = v.y

                // X (Pitch) Rotation
                val cp = cos(pitch)
                val sp = sin(pitch)
                val x2 = x1
                val y2 = y1 * cp - z1 * sp
                val z2 = y1 * sp + z1 * cp

                // Perspective projection factor
                val d = 2.5f
                val distZ = d + z2
                val coef = if (distZ > 0.1f) 1.5f / distZ else 15f
                val scaleFactor = minOf(viewWidth, viewHeight) * 0.42f

                val screenX = centerX + x2 * coef * scaleFactor
                val screenY = centerY + y2 * coef * scaleFactor
                ProjectedVertex(screenX, screenY, z2, v.r, v.g, v.b)
            }

            // Draw based on chosen style
            when (renderMode) {
                RenderMode.POINT_CLOUD -> {
                    val ptPaint = Paint().apply { isAntiAlias = true }
                    for (p in projected) {
                        ptPaint.color = AndroidColor.rgb(p.r, p.g, p.b)
                        canvas.drawCircle(p.screenX, p.screenY, 3.5f, ptPaint)
                    }
                }
                RenderMode.WIREFRAME -> {
                    val linePaint = Paint().apply {
                        isAntiAlias = true
                        style = Paint.Style.STROKE
                        strokeWidth = 1f
                    }
                    for (f in triangles) {
                        val p1 = projected[f.v1]
                        val p2 = projected[f.v2]
                        val p3 = projected[f.v3]
                        linePaint.color = AndroidColor.rgb(p1.r, p1.g, p1.b)
                        canvas.drawLine(p1.screenX, p1.screenY, p2.screenX, p2.screenY, linePaint)
                        canvas.drawLine(p2.screenX, p2.screenY, p3.screenX, p3.screenY, linePaint)
                        canvas.drawLine(p3.screenX, p3.screenY, p1.screenX, p1.screenY, linePaint)
                    }
                }
                RenderMode.TEXTURE, RenderMode.SHADED -> {
                    val polyPaint = Paint().apply {
                        isAntiAlias = true
                        style = Paint.Style.FILL
                    }
                    val path = AndroidPath()

                    // Painter's algorithm depth sorting
                    val sorted = triangles.map { t ->
                        val d = (projected[t.v1].depth + projected[t.v2].depth + projected[t.v3].depth) / 3f
                        t.copy(depth = d)
                    }.sortedBy { it.depth }

                    val baseColorHex = when (materialIndex) {
                        0 -> "#EADDFF" // Lavender Clay
                        1 -> "#CD7F32" // Copper Oxide
                        else -> "#6750A4" // Signature Minimal Purple
                    }
                    val baseColor = AndroidColor.parseColor(baseColorHex)

                    for (f in sorted) {
                        val p1 = projected[f.v1]
                        val p2 = projected[f.v2]
                        val p3 = projected[f.v3]

                        val lightFactor = Matrix3D.calculateLighting(vertices[f.v1], vertices[f.v2], vertices[f.v3])

                        val finalColor = if (renderMode == RenderMode.TEXTURE) {
                            val r = ((p1.r + p2.r + p3.r) / 3f * lightFactor).toInt().coerceIn(0, 255)
                            val g = ((p1.g + p2.g + p3.g) / 3f * lightFactor).toInt().coerceIn(0, 255)
                            val b = ((p1.b + p2.b + p3.b) / 3f * lightFactor).toInt().coerceIn(0, 255)
                            AndroidColor.rgb(r, g, b)
                        } else {
                            val r = ((AndroidColor.red(baseColor)) * lightFactor).toInt().coerceIn(0, 255)
                            val g = ((AndroidColor.green(baseColor)) * lightFactor).toInt().coerceIn(0, 255)
                            val b = ((AndroidColor.blue(baseColor)) * lightFactor).toInt().coerceIn(0, 255)
                            AndroidColor.rgb(r, g, b)
                        }

                        polyPaint.color = finalColor
                        path.reset()
                        path.moveTo(p1.screenX, p1.screenY)
                        path.lineTo(p2.screenX, p2.screenY)
                        path.lineTo(p3.screenX, p3.screenY)
                        path.close()
                        canvas.drawPath(path, polyPaint)
                    }
                }
            }
        }

        // Project Left View and Right View
        drawIndividualEye(leftCenterX, leftYaw)
        drawIndividualEye(rightCenterX, rightYaw)

        // Draw HUD details overlay
        val hudTextPaint = Paint().apply {
            isAntiAlias = true
            color = AndroidColor.parseColor("#6750A4")
            textSize = 18f
            typeface = Typeface.MONOSPACE
        }

        val hudLabelPaint = Paint().apply {
            isAntiAlias = true
            color = AndroidColor.parseColor("#7A757F")
            textSize = 15f
            typeface = Typeface.MONOSPACE
        }

        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = AndroidColor.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        // Side Labels
        canvas.drawText("LEFT EYE [Yaw Offset -0.05]", 50f, 60f, hudTextPaint)
        canvas.drawText("RIGHT EYE [Yaw Offset +0.05]", 650f, 60f, hudTextPaint)

        // Base HUD Watermarks
        canvas.drawText("PHOTO-3D STEREOGRAM RENDER", 50f, 650f, titlePaint)
        canvas.drawText(
            "Format: SBS Parallel Stereo (Side-by-Side) | Mesh Grid: 64x64\nStrength: ${String.format("%.2f", depthScale * 10f)}x | Pitch: ${String.format("%.2f", pitch)}r",
            50f,
            680f,
            hudLabelPaint
        )

        // Divide Line
        val dividePaint = Paint().apply {
            color = AndroidColor.parseColor("#6750A4")
            strokeWidth = 2f
        }
        canvas.drawLine(600f, 0f, 600f, height.toFloat(), dividePaint)

        // Framing border
        val framePaint = Paint().apply {
            color = AndroidColor.parseColor("#6750A4")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), framePaint)


        return bmp
    }

    // Saves a resulting Bitmap directly into the system Photo Album / MediaStore securely
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String): Uri? {
        val filename = "Stereo3D_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PhotoTo3D")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        if (uri != null) {
            try {
                resolver.openOutputStream(uri).use { out ->
                    if (out != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                return uri
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
            }
        }
        return null
    }
}
