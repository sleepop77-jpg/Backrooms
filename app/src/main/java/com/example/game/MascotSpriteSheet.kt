package com.example.game

import android.graphics.Bitmap
import android.graphics.Rect
import com.example.model.MascotState

class MascotSpriteSheet(private val spriteSheet: Bitmap?) {
    // 5 cols x 3 rows grid
    val cols = 5
    val rows = 3

    val frameWidth: Int = if (spriteSheet != null && spriteSheet.width > 0) spriteSheet.width / cols else 64
    val frameHeight: Int = if (spriteSheet != null && spriteSheet.height > 0) spriteSheet.height / rows else 64

    // Frame mappings (Row, Col) based on Image A
    // Row 0: Idle / Walk cycle
    // Row 1: Idle variant / Run cycle / Crouch
    // Row 2: Glitch variants / Static noise / Panic
    fun getSourceRect(state: MascotState, animTick: Long): Rect {
        if (spriteSheet == null) return Rect(0, 0, frameWidth, frameHeight)

        val (row, col) = when (state) {
            MascotState.IDLE -> Pair(0, 0)
            MascotState.WALK -> {
                // 4-frame lateral walk cycle: frames 1, 2, 3, 4 of Row 0
                val walkFrame = (1 + ((animTick / 8) % 4)).toInt()
                Pair(0, walkFrame)
            }
            MascotState.RUN -> {
                // 2-frame forward-leaning sprint: frames 1, 2 of Row 1
                val runFrame = (1 + ((animTick / 5) % 2)).toInt()
                Pair(1, runFrame)
            }
            MascotState.CROUCH -> Pair(1, 3)
            MascotState.PANIC -> Pair(2, 4) // row 2, col 4 with sweat & question marks
            MascotState.GLITCH -> {
                // Rapid flicker through row 2 glitch frames (0, 1, 2, 3)
                val glitchFrame = ((animTick / 3) % 4).toInt()
                Pair(2, glitchFrame)
            }
            MascotState.DEAD -> Pair(2, 2)
        }

        val left = (col * frameWidth).coerceIn(0, spriteSheet.width - frameWidth)
        val top = (row * frameHeight).coerceIn(0, spriteSheet.height - frameHeight)
        return Rect(left, top, left + frameWidth, top + frameHeight)
    }
}
