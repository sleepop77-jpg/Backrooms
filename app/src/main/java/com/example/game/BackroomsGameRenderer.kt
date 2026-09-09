package com.example.game

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import com.example.model.DayStats
import com.example.model.MascotState
import com.example.model.RunPhase
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin

class BackroomsGameRenderer(
    private val wireframeBitmap: Bitmap?
) {
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD54F")
        textSize = 24f
    }
    private val redStrobePaint = Paint().apply { color = Color.parseColor("#88FF1744") }
    private val staticPaint = Paint().apply { color = Color.WHITE }
    private val bgPaint = Paint().apply { color = Color.parseColor("#070604") }
    private val wallPaint = Paint().apply {
        color = Color.parseColor("#0A0906")
        style = Paint.Style.STROKE
        strokeWidth = 26f
    }
    private val wallInnerPaint = Paint().apply {
        color = Color.parseColor("#6B6230")
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    private val roomWallPaint = Paint().apply {
        color = Color.parseColor("#0A0906")
        style = Paint.Style.STROKE
        strokeWidth = 18f
    }
    private val roomInnerPaint = Paint().apply {
        color = Color.parseColor("#6B6230")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val lightPaint = Paint().apply { color = Color.parseColor("#E8F5C8") }
    private val lightGlowPaint = Paint().apply { color = Color.parseColor("#22E8F5C8") }
    private val columnPaint = Paint().apply { color = Color.parseColor("#15120A") }
    private val columnEdgePaint = Paint().apply {
        color = Color.parseColor("#6B6230")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val gatePaint = Paint().apply {
        color = Color.parseColor("#55FFD54F")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private val gatePanelPaint = Paint().apply { color = Color.parseColor("#DD18170F") }
    private val gatePanelBorder = Paint().apply {
        color = Color.parseColor("#FFD54F")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#BBC7A500")
        textSize = 40f
        textAlign = Paint.Align.CENTER
    }
    private val floorPaints = Array(8) { i -> Paint().apply { color = sectorFloor(i) } }
    private val roomFloorPaints = Array(8) { i -> Paint().apply { color = sectorRoomFloor(i) } }

    private fun sectorFloor(sector: Int): Int {
        val t = sector / 7f
        fun l(a: Int, b: Int) = (a + (b - a) * t).toInt()
        return Color.rgb(l(0x3A, 0x26), l(0x33, 0x1F), l(0x20, 0x11))
    }

    private fun sectorRoomFloor(sector: Int): Int {
        val t = sector / 7f
        fun l(a: Int, b: Int) = (a + (b - a) * t).toInt()
        return Color.rgb(l(0x33, 0x20), l(0x2C, 0x1A), l(0x1B, 0x0E))
    }

    fun render(
        canvas: Canvas,
        viewWidth: Float,
        viewHeight: Float,
        dayStats: DayStats,
        animTick: Long
    ) {
        val level = dayStats.currentLevel
        val totalSec = dayStats.bankedSeconds +
            if (dayStats.runPhase == RunPhase.STUDY_ACTIVE) dayStats.currentBlockSeconds else 0L
        val worldX = totalSec * BackroomsWorld.PX_PER_SEC
        val centerY = BackroomsWorld.corridorCenterAt(worldX, level)
        val scale = viewHeight / BackroomsWorld.WORLD_HEIGHT
        val camLeft = worldX - viewWidth / (2f * scale)

        canvas.save()
        canvas.scale(scale, scale)
        canvas.translate(-camLeft, 0f)
        val left = camLeft - 200f
        val right = camLeft + viewWidth / scale + 200f

        drawWorld(canvas, left, right, level)
        drawGates(canvas, left, right, level)
        drawExit(canvas, level, totalSec)
        drawMascot(canvas, worldX, centerY, dayStats, animTick)
        if (dayStats.runPhase == RunPhase.BREACH_KILLED || dayStats.isBlacklistBreachGraceActive) {
            drawWireframeLunge(canvas, worldX, centerY, dayStats, animTick)
        }
        canvas.restore()
        drawScreenOverlays(canvas, viewWidth, viewHeight, dayStats, animTick, totalSec, worldX)
    }

    private fun drawWorld(canvas: Canvas, left: Float, right: Float, level: Int) {
        canvas.drawRect(left, 0f, right, BackroomsWorld.WORLD_HEIGHT, bgPaint)
        val half = BackroomsWorld.CORRIDOR_HALF
        var x = left - (left % 32f)
        while (x < right) {
            val cy = BackroomsWorld.corridorCenterAt(x, level)
            canvas.drawRect(x, cy - half, x + 34f, cy + half, floorPaints[BackroomsWorld.sectorOf(x)])
            x += 32f
        }
        val topPath = Path()
        val botPath = Path()
        var sx = left
        var first = true
        while (sx <= right) {
            val cy = BackroomsWorld.corridorCenterAt(sx, level)
            if (first) {
                topPath.moveTo(sx, cy - half)
                botPath.moveTo(sx, cy + half)
                first = false
            } else {
                topPath.lineTo(sx, cy - half)
                botPath.lineTo(sx, cy + half)
            }
            sx += 48f
        }
        canvas.drawPath(topPath, wallPaint)
        canvas.drawPath(botPath, wallPaint)
        canvas.drawPath(topPath, wallInnerPaint)
        canvas.drawPath(botPath, wallInnerPaint)

        val c0 = floor(left / BackroomsWorld.CHUNK).toInt()
        val c1 = floor(right / BackroomsWorld.CHUNK).toInt()
        for (c in c0..c1) {
            val geo = BackroomsWorld.geometry(c, level)
            for (room in geo.rooms) {
                val sector = BackroomsWorld.sectorOf((room.left + room.right) / 2f)
                val r = RectF(room.left, room.top, room.right, room.bottom)
                canvas.drawRect(r, roomFloorPaints[sector])
                canvas.drawRect(r, roomWallPaint)
                canvas.drawRect(r, roomInnerPaint)
                val doorSideY = if (room.above) r.bottom else r.top
                canvas.drawRect(
                    RectF(room.doorX - 60f, doorSideY - 14f, room.doorX + 60f, doorSideY + 14f),
                    floorPaints[sector]
                )
                val lw = r.width() * 0.2f
                canvas.drawRect(
                    RectF(r.centerX() - lw / 2f, r.top + 12f, r.centerX() + lw / 2f, r.top + 22f),
                    lightPaint
                )
            }
            for (lx in geo.lightXs) {
                val cy = BackroomsWorld.corridorCenterAt(lx, level)
                canvas.drawRect(RectF(lx - 12f, cy - half - 44f, lx + 82f, cy - half + 6f), lightGlowPaint)
                canvas.drawRect(RectF(lx, cy - half - 26f, lx + 70f, cy - half - 10f), lightPaint)
            }
            for (cx in geo.columns) {
                val cy = BackroomsWorld.corridorCenterAt(cx, level)
                val topCol = RectF(cx - 24f, cy - 122f, cx + 24f, cy - 74f)
                val botCol = RectF(cx - 24f, cy + 74f, cx + 24f, cy + 122f)
                canvas.drawRect(topCol, columnPaint)
                canvas.drawRect(topCol, columnEdgePaint)
                canvas.drawRect(botCol, columnPaint)
                canvas.drawRect(botCol, columnEdgePaint)
            }
        }
    }

    private fun drawGates(canvas: Canvas, left: Float, right: Float, level: Int) {
        val half = BackroomsWorld.CORRIDOR_HALF
        for (s in 0..8) {
            val gx = s * BackroomsWorld.WORLD_LENGTH / 8f
            if (gx < left || gx > right) continue
            val cy = BackroomsWorld.corridorCenterAt(gx, level)
            canvas.drawLine(gx, cy - half - 240f, gx, cy + half + 240f, gatePaint)
            val panel = RectF(gx - 170f, cy - half - 330f, gx + 170f, cy - half - 258f)
            canvas.drawRect(panel, gatePanelPaint)
            canvas.drawRect(panel, gatePanelBorder)
            canvas.drawText(if (s == 8) "FIRE EXIT" else "HOUR $s", gx, cy - half - 278f, labelPaint)
        }
    }

    private fun drawExit(canvas: Canvas, level: Int, totalSec: Long) {
        val half = BackroomsWorld.CORRIDOR_HALF
        val ex = BackroomsWorld.WORLD_LENGTH
        val cy = BackroomsWorld.corridorCenterAt(ex, level)
        val open = totalSec >= BackroomsWorld.TOTAL_SECONDS
        val doorPaint = Paint().apply { color = if (open) Color.parseColor("#00E676") else Color.parseColor("#B71C1C") }
        canvas.drawRect(RectF(ex - 20f, cy - half, ex + 20f, cy + half), doorPaint)
        val signPaint = Paint().apply { color = if (open) Color.parseColor("#69F0AE") else Color.parseColor("#FF5252") }
        canvas.drawRect(RectF(ex - 90f, cy - half - 70f, ex + 90f, cy - half - 30f), signPaint)
        textPaint.textSize = 26f
        textPaint.color = Color.BLACK
        canvas.drawText("EXIT", ex, cy - half - 40f, textPaint)
    }

    private fun drawMascot(
        canvas: Canvas,
        worldX: Float,
        centerY: Float,
        dayStats: DayStats,
        animTick: Long
    ) {
        val state = dayStats.mascotState
        val isMoving = (state == MascotState.WALK || state == MascotState.RUN)
        val feetY = centerY + 60f
        val bobShadow = if (isMoving) abs(sin(animTick * 0.25f)) * 6f else 0f

        val shadowPaint = Paint().apply {
            color = Color.argb(130, 0, 0, 0)
            style = Paint.Style.FILL
        }
        val shadowW = if (isMoving) 90f - bobShadow else 100f
        canvas.drawOval(
            RectF(worldX - shadowW / 2f, feetY - 6f, worldX + shadowW / 2f, feetY + 16f),
            shadowPaint
        )

        MascotPainter.draw(
            canvas = canvas,
            x = worldX,
            feetY = feetY,
            height = 150f,
            state = state,
            animTick = animTick,
            facingRight = true
        )

        val badgePaint = Paint().apply { color = Color.parseColor("#CC12110B") }
        val badgeBorder = Paint().apply {
            color = Color.parseColor("#FFD54F")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val bRect = RectF(worldX - 50f, feetY - 190f, worldX + 50f, feetY - 160f)
        canvas.drawRect(bRect, badgePaint)
        canvas.drawRect(bRect, badgeBorder)
        textPaint.textSize = 18f
        textPaint.color = Color.parseColor("#FFD54F")
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("PLAYER", worldX, feetY - 168f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawWireframeLunge(
        canvas: Canvas,
        worldX: Float,
        centerY: Float,
        dayStats: DayStats,
        animTick: Long
    ) {
        if (wireframeBitmap == null || wireframeBitmap.isRecycled) return
        val scale = if (dayStats.runPhase == RunPhase.BREACH_KILLED) {
            1.5f + ((animTick % 60) * 0.03f)
        } else {
            0.95f
        }
        val entityW = 240f * scale
        val entityH = 380f * scale
        val entityX = worldX + 180f - (if (dayStats.runPhase == RunPhase.BREACH_KILLED) (animTick % 30) * 8f else 0f)
        val entityY = centerY + 80f
        val dst = RectF(entityX - entityW / 2f, entityY - entityH, entityX + entityW / 2f, entityY)
        val src = Rect(0, 0, wireframeBitmap.width, wireframeBitmap.height)
        canvas.drawBitmap(wireframeBitmap, src, dst, null)
    }

    private fun drawScreenOverlays(
        canvas: Canvas,
        viewWidth: Float,
        viewHeight: Float,
        dayStats: DayStats,
        animTick: Long,
        totalSec: Long,
        worldX: Float
    ) {
        if (dayStats.isBlacklistBreachGraceActive || dayStats.runPhase == RunPhase.BREACH_KILLED) {
            if ((animTick / 8) % 2 == 0L) {
                canvas.drawRect(0f, 0f, viewWidth, viewHeight, redStrobePaint)
            }
        }
        val scanPaint = Paint().apply {
            color = Color.argb(25, 0, 0, 0)
            strokeWidth = 2f
        }
        var y = 0f
        while (y < viewHeight) {
            canvas.drawLine(0f, y, viewWidth, y, scanPaint)
            y += 5f
        }
        if (dayStats.mascotState == MascotState.GLITCH || dayStats.runPhase == RunPhase.BREACH_KILLED) {
            for (i in 0..100) {
                val rx = (animTick * 41 + i * 89) % viewWidth.toInt()
                val ry = (animTick * 37 + i * 113) % viewHeight.toInt()
                canvas.drawCircle(rx.toFloat(), ry.toFloat(), 2.5f, staticPaint)
            }
        }
        val sector = (totalSec / 3600).coerceIn(0, 8)
        val sectorBadgePaint = Paint().apply { color = Color.parseColor("#DD18170F") }
        val sectorBorderPaint = Paint().apply {
            color = Color.parseColor("#FFD54F")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val sRect = RectF(20f, 20f, 430f, 65f)
        canvas.drawRect(sRect, sectorBadgePaint)
        canvas.drawRect(sRect, sectorBorderPaint)
        textPaint.textSize = 20f
        textPaint.color = Color.parseColor("#FFD54F")
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("LEVEL ${dayStats.currentLevel}: SECTOR 0$sector / 08", 35f, 48f, textPaint)
        val tRect = RectF(20f, 75f, 430f, 115f)
        canvas.drawRect(tRect, sectorBadgePaint)
        canvas.drawRect(tRect, sectorBorderPaint)
        textPaint.textSize = 17f
        canvas.drawText(String.format("TREK: %.2f / 20.74 km", worldX / 100000f), 35f, 101f, textPaint)
        if (totalSec >= 28800L) {
            textPaint.color = Color.parseColor("#00E676")
            textPaint.textSize = 22f
            canvas.drawText("FIRE EXIT REACHED - RUN COMPLETE", 35f, 150f, textPaint)
        }
    }
}
