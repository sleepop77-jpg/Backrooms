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
import kotlin.math.sin

class BackroomsGameRenderer(
    private val mascotSheetBitmap: Bitmap?,
    private val wireframeBitmap: Bitmap?,
    private val mapStripBitmap: Bitmap?
) {
    private val spriteSheet = MascotSpriteSheet(mascotSheetBitmap)

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD54F")
        textSize = 24f
    }
    private val redStrobePaint = Paint().apply {
        color = Color.parseColor("#88FF1744")
    }
    private val staticPaint = Paint().apply {
        color = Color.WHITE
    }
    private val pathLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#44FFD54F")
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }
    private val pathDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#88FFD54F")
        style = Paint.Style.FILL
    }
    private val passedPathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9900E676")
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    fun render(
        canvas: Canvas,
        viewWidth: Float,
        viewHeight: Float,
        dayStats: DayStats,
        animTick: Long
    ) {
        // Total study progress (8 hours = 28,800 seconds)
        val totalBanked = dayStats.bankedSeconds + if (dayStats.runPhase == RunPhase.STUDY_ACTIVE) dayStats.currentBlockSeconds else 0L
        val progressRatio = (totalBanked.toFloat() / 28800f).coerceIn(0f, 1f)

        // Calculate mascot's current position and direction on the 2D Backrooms map
        val placement = BackroomsMapData.getPositionAndDirection(progressRatio)

        // 1. World Canvas Camera Transformation
        // The 2D map is larger than the viewport. The camera smoothly follows the mascot
        val mapPixelWidth = if (mapStripBitmap != null && !mapStripBitmap.isRecycled) {
            viewHeight * 2.2f * (mapStripBitmap.width.toFloat() / mapStripBitmap.height.toFloat())
        } else {
            viewWidth * 3.0f
        }
        val mapPixelHeight = viewHeight * 2.2f

        // Mascot absolute coordinate in map space
        val mascotMapX = placement.mapNormX * mapPixelWidth
        val mascotMapY = placement.mapNormY * mapPixelHeight

        // Center camera around mascot, clamped within map boundaries
        val cameraX = (mascotMapX - viewWidth / 2f).coerceIn(0f, (mapPixelWidth - viewWidth).coerceAtLeast(0f))
        val cameraY = (mascotMapY - viewHeight / 2f).coerceIn(0f, (mapPixelHeight - viewHeight).coerceAtLeast(0f))

        canvas.save()
        // Translate view to camera coordinate
        canvas.translate(-cameraX, -cameraY)

        // Draw 2D Top-Down/Tactical Backrooms Map Layout
        draw2DBackroomsMap(canvas, mapPixelWidth, mapPixelHeight, progressRatio)

        // Draw 2D Waypoint Navigation Paths & Hour Sector Markers
        drawWaypointLines(canvas, mapPixelWidth, mapPixelHeight, progressRatio)

        // Draw Exit Door / Escape Gate at the end of the map (Sector 8)
        drawExitGate(canvas, mapPixelWidth, mapPixelHeight, progressRatio, animTick)

        // Draw Player Mascot with complete Among Us waddle-bob animation & directional mirroring
        drawAmongUsMascot(
            canvas = canvas,
            mascotMapX = mascotMapX,
            mascotMapY = mascotMapY,
            facingRight = placement.facingRight,
            dayStats = dayStats,
            animTick = animTick
        )

        // Draw The Wireframe Entity during active breach or death sequence
        if (dayStats.runPhase == RunPhase.BREACH_KILLED || dayStats.isBlacklistBreachGraceActive) {
            drawWireframeLunge(
                canvas = canvas,
                mascotMapX = mascotMapX,
                mascotMapY = mascotMapY,
                dayStats = dayStats,
                animTick = animTick
            )
        }

        canvas.restore()

        // Screen-space Overlays: CRT Scanlines, Strobe, and HUD Overlays
        drawScreenOverlays(canvas, viewWidth, viewHeight, dayStats, animTick, totalBanked)
    }

    private fun draw2DBackroomsMap(
        canvas: Canvas,
        mapWidth: Float,
        mapHeight: Float,
        progressRatio: Float
    ) {
        // Base dark liminal floor
        val basePaint = Paint().apply { color = Color.parseColor("#14130A") }
        canvas.drawRect(0f, 0f, mapWidth, mapHeight, basePaint)

        if (mapStripBitmap != null && !mapStripBitmap.isRecycled) {
            val src = Rect(0, 0, mapStripBitmap.width, mapStripBitmap.height)
            val dst = RectF(0f, 0f, mapWidth, mapHeight)
            canvas.drawBitmap(mapStripBitmap, src, dst, null)
        } else {
            // High-detail 2D tactical room grid fallback
            val roomPaint = Paint().apply { color = Color.parseColor("#262211") }
            val borderPaint = Paint().apply {
                color = Color.parseColor("#52471B")
                style = Paint.Style.STROKE
                strokeWidth = 8f
            }
            for (col in 0..8) {
                val left = col * (mapWidth / 9f) + 15f
                val right = (col + 1) * (mapWidth / 9f) - 15f
                canvas.drawRect(left, 40f, right, mapHeight - 40f, roomPaint)
                canvas.drawRect(left, 40f, right, mapHeight - 40f, borderPaint)
            }
        }
    }

    private fun drawWaypointLines(
        canvas: Canvas,
        mapWidth: Float,
        mapHeight: Float,
        progressRatio: Float
    ) {
        val waypoints = BackroomsMapData.waypoints
        if (waypoints.size < 2) return

        // 1. Draw entire planned path (dim dashed/dotted line)
        val fullPath = Path()
        for (i in waypoints.indices) {
            val wx = waypoints[i].x * mapWidth
            val wy = waypoints[i].y * mapHeight
            if (i == 0) fullPath.moveTo(wx, wy) else fullPath.lineTo(wx, wy)
            // Sector nodes
            canvas.drawCircle(wx, wy, 8f, pathDotPaint)
            if (i in listOf(0, 3, 6, 9, 12)) {
                val hour = (i * 8 / 12).coerceIn(0, 8)
                textPaint.textSize = 20f
                textPaint.color = Color.parseColor("#88FFD54F")
                canvas.drawText("${hour}H", wx - 14f, wy - 14f, textPaint)
            }
        }
        canvas.drawPath(fullPath, pathLinePaint)
    }

    private fun drawExitGate(
        canvas: Canvas,
        mapWidth: Float,
        mapHeight: Float,
        progressRatio: Float,
        animTick: Long
    ) {
        val exitWp = BackroomsMapData.waypoints.last()
        val exitX = exitWp.x * mapWidth
        val exitY = exitWp.y * mapHeight

        // Exit Door Visual (Steel Red Industrial Fire Door with illuminated EXIT sign)
        val doorPaint = Paint().apply {
            color = if (progressRatio >= 1.0f) Color.parseColor("#00E676") else Color.parseColor("#B71C1C")
        }
        val doorRect = RectF(exitX - 25f, exitY - 50f, exitX + 25f, exitY + 10f)
        canvas.drawRoundRect(doorRect, 6f, 6f, doorPaint)

        // EXIT Sign with pulsing glow
        val signPaint = Paint().apply {
            color = if (progressRatio >= 1.0f) Color.parseColor("#69F0AE") else Color.parseColor("#FF5252")
            style = Paint.Style.FILL
        }
        canvas.drawRect(exitX - 20f, exitY - 70f, exitX + 20f, exitY - 54f, signPaint)
        textPaint.textSize = 14f
        textPaint.color = Color.BLACK
        canvas.drawText("EXIT", exitX - 16f, exitY - 57f, textPaint)
    }

    private fun drawAmongUsMascot(
        canvas: Canvas,
        mascotMapX: Float,
        mascotMapY: Float,
        facingRight: Boolean,
        dayStats: DayStats,
        animTick: Long
    ) {
        val state = dayStats.mascotState
        val isMoving = (state == MascotState.WALK || state == MascotState.RUN)

        // Among Us movement dynamics:
        // Pure sinusoidal vertical waddle bob: yBob = -A * |sin(2 * pi * f * t)|
        val bobFreq = if (state == MascotState.RUN) 0.22f else 0.14f
        val bobAmp = if (state == MascotState.RUN) 16f else 10f
        val bobY = if (isMoving) -abs(sin(animTick * bobFreq) * bobAmp) else 0f

        // Panic horizontal jitter
        val jitterX = if (state == MascotState.PANIC || state == MascotState.GLITCH) {
            ((animTick % 5) - 2) * 6f
        } else 0f

        val currentX = mascotMapX + jitterX
        val currentY = mascotMapY + bobY

        // Shadow beneath mascot
        val shadowPaint = Paint().apply {
            color = Color.argb(130, 0, 0, 0)
            style = Paint.Style.FILL
        }
        val shadowW = if (isMoving) 70f - (bobY * 0.5f) else 80f
        canvas.drawOval(
            RectF(currentX - shadowW / 2f, mascotMapY - 4f, currentX + shadowW / 2f, mascotMapY + 14f),
            shadowPaint
        )

        val mascotDisplayW = 120f
        val mascotDisplayH = 120f

        canvas.save()

        // Directional mirroring (flip horizontally if facing left)
        if (!facingRight) {
            canvas.scale(-1f, 1f, currentX, currentY)
        }

        // Forward lean during RUN (Among Us sprint)
        if (state == MascotState.RUN) {
            canvas.rotate(8f, currentX, currentY)
        }

        // Crouch pose translation
        if (state == MascotState.CROUCH) {
            canvas.translate(0f, 12f)
        }

        val dstRect = RectF(
            currentX - mascotDisplayW / 2f,
            currentY - mascotDisplayH,
            currentX + mascotDisplayW / 2f,
            currentY
        )

        if (mascotSheetBitmap != null && !mascotSheetBitmap.isRecycled) {
            val srcRect = spriteSheet.getSourceRect(state, animTick)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(mascotSheetBitmap, srcRect, dstRect, paint)
        } else {
            // Clean vector backup
            val bodyPaint = Paint().apply { color = Color.parseColor("#FFD54F") }
            val visorPaint = Paint().apply { color = Color.parseColor("#15140D") }
            canvas.drawRoundRect(dstRect, 22f, 22f, bodyPaint)
            val vRect = RectF(dstRect.left + 24f, dstRect.top + 22f, dstRect.right - 14f, dstRect.top + 55f)
            canvas.drawRoundRect(vRect, 10f, 10f, visorPaint)
        }

        canvas.restore()

        // Player HUD Badge over mascot head
        val badgePaint = Paint().apply {
            color = Color.parseColor("#CC12110B")
            style = Paint.Style.FILL
        }
        val badgeBorder = Paint().apply {
            color = Color.parseColor("#FFD54F")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val bW = 86f
        val bH = 26f
        val bRect = RectF(currentX - bW / 2f, currentY - mascotDisplayH - 32f, currentX + bW / 2f, currentY - mascotDisplayH - 6f)
        canvas.drawRect(bRect, badgePaint)
        canvas.drawRect(bRect, badgeBorder)

        textPaint.textSize = 16f
        textPaint.color = Color.parseColor("#FFD54F")
        canvas.drawText("PLAYER", currentX - 30f, currentY - mascotDisplayH - 12f, textPaint)
    }

    private fun drawWireframeLunge(
        canvas: Canvas,
        mascotMapX: Float,
        mascotMapY: Float,
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
        val entityX = mascotMapX + 160f - (if (dayStats.runPhase == RunPhase.BREACH_KILLED) (animTick % 30) * 8f else 0f)
        val entityY = mascotMapY + 20f

        val dst = RectF(
            entityX - entityW / 2f,
            entityY - entityH,
            entityX + entityW / 2f,
            entityY
        )
        val src = Rect(0, 0, wireframeBitmap.width, wireframeBitmap.height)
        canvas.drawBitmap(wireframeBitmap, src, dst, null)
    }

    private fun drawScreenOverlays(
        canvas: Canvas,
        viewWidth: Float,
        viewHeight: Float,
        dayStats: DayStats,
        animTick: Long,
        totalBanked: Long
    ) {
        // Red strobe when grace timer or kill is triggered
        if (dayStats.isBlacklistBreachGraceActive || dayStats.runPhase == RunPhase.BREACH_KILLED) {
            if ((animTick / 8) % 2 == 0L) {
                canvas.drawRect(0f, 0f, viewWidth, viewHeight, redStrobePaint)
            }
        }

        // CRT horizontal scanlines
        val scanPaint = Paint().apply {
            color = Color.argb(25, 0, 0, 0)
            strokeWidth = 2f
        }
        var y = 0f
        while (y < viewHeight) {
            canvas.drawLine(0f, y, viewWidth, y, scanPaint)
            y += 5f
        }

        // CRT Glitch static dots on death or panic
        if (dayStats.mascotState == MascotState.GLITCH || dayStats.runPhase == RunPhase.BREACH_KILLED) {
            for (i in 0..100) {
                val rx = (animTick * 41 + i * 89) % viewWidth.toInt()
                val ry = (animTick * 37 + i * 113) % viewHeight.toInt()
                canvas.drawCircle(rx.toFloat(), ry.toFloat(), 2.5f, staticPaint)
            }
        }

        // Current Sector Badge in top-left
        val sector = (totalBanked / 3600).coerceIn(0, 8)
        val sectorBadgePaint = Paint().apply {
            color = Color.parseColor("#DD18170F")
            style = Paint.Style.FILL
        }
        val sectorBorderPaint = Paint().apply {
            color = Color.parseColor("#FFD54F")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val sRect = RectF(20f, 20f, 380f, 65f)
        canvas.drawRect(sRect, sectorBadgePaint)
        canvas.drawRect(sRect, sectorBorderPaint)

        textPaint.textSize = 20f
        textPaint.color = Color.parseColor("#FFD54F")
        canvas.drawText("LEVEL 0: SECTOR 0$sector / 08", 35f, 48f, textPaint)

        if (totalBanked >= 28800L) {
            textPaint.color = Color.parseColor("#00E676")
            canvas.drawText("FIRE EXIT REACHED - RUN COMPLETE", 35f, 95f, textPaint)
        }
    }
}
