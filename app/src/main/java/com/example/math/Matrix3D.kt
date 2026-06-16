package com.example.math

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vertex3D(
    val x: Float,
    val y: Float,
    val z: Float,
    val r: Int = 128,
    val g: Int = 128,
    val b: Int = 128
)

data class ProjectedVertex(
    val screenX: Float,
    val screenY: Float,
    val depth: Float, // Rotated Z value for painter's depth sorting
    val r: Int,
    val g: Int,
    val b: Int
)

data class Triangle3D(
    val v1: Int,
    val v2: Int,
    val v3: Int,
    var depth: Float = 0f // Average depth of the 3 vertices for painter's algorithm sorting
)

object Matrix3D {
    // Rotates and projects a 3D vertex onto 2D screen coordinates
    fun project(
        vertex: Vertex3D,
        yawRad: Float,
        pitchRad: Float,
        width: Float,
        height: Float,
        zoom: Float,
        cameraDistance: Float = 2.5f
    ): ProjectedVertex {
        // Rotate Y (Yaw)
        val cy = cos(yawRad)
        val sy = sin(yawRad)
        val x1 = vertex.x * cy - vertex.z * sy
        val z1 = vertex.x * sy + vertex.z * cy
        val y1 = vertex.y

        // Rotate X (Pitch)
        val cp = cos(pitchRad)
        val sp = sin(pitchRad)
        val x2 = x1
        val y2 = y1 * cp - z1 * sp
        val z2 = y1 * sp + z1 * cp

        // Perspective Projection
        // cameraDistance must be > 1 to avoid clipping or infinite scaling near the screen plane
        val distZ = cameraDistance + z2
        val projection = if (distZ > 0.1f) 1.5f / distZ else 15f
        
        // Multiplier based on minimum screen dimension to scale model proportionately
        val sizeMultiplier = minOf(width, height) * 0.45f * zoom

        val screenX = width / 2f + x2 * projection * sizeMultiplier
        val screenY = height / 2f + y2 * projection * sizeMultiplier

        return ProjectedVertex(
            screenX = screenX,
            screenY = screenY,
            depth = z2,
            r = vertex.r,
            g = vertex.g,
            b = vertex.b
        )
    }

    // Calculates face normal and computes Lambertian diffuse lighting intensity
    fun calculateLighting(
        v1: Vertex3D,
        v2: Vertex3D,
        v3: Vertex3D,
        lightDirection: FloatArray = floatArrayOf(0.5f, -0.7f, -0.5f) // Normalized virtual light source
    ): Float {
        // Normal of the face: Vector Cross Product (v2 - v1) x (v3 - v1)
        val ax = v2.x - v1.x
        val ay = v2.y - v1.y
        val az = v2.z - v1.z

        val bx = v3.x - v1.x
        val by = v3.y - v1.y
        val bz = v3.z - v1.z

        var nx = ay * bz - az * by
        var ny = az * bx - ax * bz
        var nz = ax * by - ay * bx

        // Normalize
        val length = sqrt(nx * nx + ny * ny + nz * nz)
        if (length > 0f) {
            nx /= length
            ny /= length
            nz /= length
        } else {
            ny = -1f // Default upward normal
        }

        // Dot product of normal and light direction
        val dot = nx * lightDirection[0] + ny * lightDirection[1] + nz * lightDirection[2]
        
        // Return computed light intensity factor (bounded between 0.25 ambient and 1.0 peak)
        return 0.25f + 0.75f * maxOf(0f, dot)
    }
}
