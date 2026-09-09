package com.example.game

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.random.Random

object BackroomsWorld {
    const val WORLD_HEIGHT = 1080f
    const val CORRIDOR_HALF = 170f
    const val CHUNK = 1600f
    const val PX_PER_SEC = 72f
    const val TOTAL_SECONDS = 28800L
    val WORLD_LENGTH: Float = TOTAL_SECONDS * PX_PER_SEC
    val CHUNK_COUNT: Int = (WORLD_LENGTH / CHUNK).toInt()

    fun sectorOf(worldX: Float): Int = ((worldX / WORLD_LENGTH) * 8f).toInt().coerceIn(0, 7)

    fun meanderOffset(chunk: Int, level: Int): Float {
        val r = Random(level * 337L + chunk * 6151L + 7L)
        return (r.nextFloat() - 0.5f) * 300f
    }

    fun corridorCenterAt(x: Float, level: Int): Float {
        val cf = x / CHUNK
        val i = floor(cf).toInt()
        val t = cf - i
        val a = meanderOffset(i, level)
        val b = meanderOffset(i + 1, level)
        val u = (1f - cos(t * PI.toFloat())) / 2f
        return WORLD_HEIGHT / 2f + a + (b - a) * u
    }

    data class Room(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val doorX: Float,
        val above: Boolean
    )

    data class ChunkGeometry(
        val rooms: List<Room>,
        val columns: List<Float>,
        val lightXs: List<Float>
    )

    fun geometry(chunk: Int, level: Int): ChunkGeometry {
        val r = Random(level * 7919L + chunk * 104729L + 13L)
        val x0 = chunk * CHUNK
        val rooms = mutableListOf<Room>()
        val roomCount = r.nextInt(1, 3)
        for (i in 0 until roomCount) {
            val above = r.nextBoolean()
            val w = r.nextFloat() * 400f + 500f
            val left = x0 + r.nextFloat() * (CHUNK - w).coerceAtLeast(1f)
            val right = left + w
            val cy = corridorCenterAt((left + right) / 2f, level)
            val h = r.nextFloat() * 120f + 240f
            val doorX = left + w * (0.3f + r.nextFloat() * 0.4f)
            if (above) {
                rooms.add(Room(left, cy - CORRIDOR_HALF - h, right, cy - CORRIDOR_HALF + 8f, doorX, true))
            } else {
                rooms.add(Room(left, cy + CORRIDOR_HALF - 8f, right, cy + CORRIDOR_HALF + h, doorX, false))
            }
        }
        val columns = mutableListOf<Float>()
        if (r.nextFloat() < 0.45f) {
            val n = r.nextInt(2, 5)
            for (i in 0 until n) columns.add(x0 + (i + 0.5f) * CHUNK / n)
        }
        val lightXs = mutableListOf<Float>()
        var lx = x0 + 120f
        while (lx < x0 + CHUNK - 80f) {
            lightXs.add(lx)
            lx += 260f + r.nextFloat() * 120f
        }
        return ChunkGeometry(rooms, columns, lightXs)
    }
}
