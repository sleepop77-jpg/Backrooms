package com.example.game

import android.graphics.PointF
import kotlin.math.sqrt

data class Waypoint(val x: Float, val y: Float)

object BackroomsMapData {
    // 2D Tactical Backrooms layout path normalized [0.0f .. 1.0f] in map space
    // 9 sectors corresponding to 0 to 8 banked study hours.
    // Hallways and turns through Level 0 rooms from Sector 0 (Entrance) to Sector 8 (Fire Exit Door)
    val waypoints = listOf(
        Waypoint(0.04f, 0.45f), // Hour 0: Entrance vent / drop zone
        Waypoint(0.12f, 0.45f), // Sector 1 corridor
        Waypoint(0.16f, 0.28f), // North corridor turn
        Waypoint(0.25f, 0.28f), // Hour 2: Fluorescent Hallway
        Waypoint(0.32f, 0.65f), // South turn into Column maze
        Waypoint(0.42f, 0.65f), // Hour 3: Column maze navigation
        Waypoint(0.48f, 0.40f), // Hour 4: Central intersection (Halfway mark)
        Waypoint(0.58f, 0.40f), // Hour 5: East office divider lane
        Waypoint(0.66f, 0.72f), // South junction
        Waypoint(0.76f, 0.72f), // Hour 6: Stained corridor
        Waypoint(0.84f, 0.48f), // North alleyway
        Waypoint(0.92f, 0.48f), // Hour 7: Shadow corridor approaching fire exit
        Waypoint(0.97f, 0.48f)  // Hour 8: Emergency Red Exit Door threshold
    )

    // Calculate position along waypoints polyline for progress t in [0.0 .. 1.0]
    fun getPositionAndDirection(progress: Float): MascotMapPlacement {
        val clampedProgress = progress.coerceIn(0f, 1f)
        if (waypoints.isEmpty()) return MascotMapPlacement(0.5f, 0.5f, facingRight = true)
        if (waypoints.size == 1) return MascotMapPlacement(waypoints[0].x, waypoints[0].y, facingRight = true)

        // Calculate segment lengths
        val segmentLengths = FloatArray(waypoints.size - 1)
        var totalLength = 0f
        for (i in 0 until waypoints.size - 1) {
            val dx = waypoints[i + 1].x - waypoints[i].x
            val dy = waypoints[i + 1].y - waypoints[i].y
            val len = sqrt(dx * dx + dy * dy)
            segmentLengths[i] = len
            totalLength += len
        }

        val targetDistance = clampedProgress * totalLength
        var accumulated = 0f

        for (i in 0 until waypoints.size - 1) {
            val segLen = segmentLengths[i]
            if (accumulated + segLen >= targetDistance || i == waypoints.size - 2) {
                val segProgress = if (segLen > 0.0001f) (targetDistance - accumulated) / segLen else 0f
                val clampedSegProgress = segProgress.coerceIn(0f, 1f)
                val pA = waypoints[i]
                val pB = waypoints[i + 1]
                val currentX = pA.x + (pB.x - pA.x) * clampedSegProgress
                val currentY = pA.y + (pB.y - pA.y) * clampedSegProgress
                val facingRight = (pB.x - pA.x) >= 0f
                return MascotMapPlacement(currentX, currentY, facingRight)
            }
            accumulated += segLen
        }

        val last = waypoints.last()
        return MascotMapPlacement(last.x, last.y, facingRight = true)
    }
}

data class MascotMapPlacement(
    val mapNormX: Float,
    val mapNormY: Float,
    val facingRight: Boolean
)
