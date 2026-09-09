package com.example.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.model.MascotState
import kotlin.math.abs
import kotlin.math.sin

object MascotPainter {

    private val suit = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFD54F") }
    private val suitShade = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D9B23A") }
    private val visorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#3B2A1D") }
    private val visorDead = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#101010") }
    private val visorGlint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6E543C") }
    private val leather = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6B4A2B") }
    private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1708")
        style = Paint.Style.STROKE
    }
    private val sweat = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#7FD4FF") }

    fun draw(
        canvas: Canvas,
        x: Float,
        feetY: Float,
        height: Float,
        state: MascotState,
        animTick: Long,
        facingRight: Boolean
    ) {
        val h = height
        canvas.save()
        canvas.translate(x, feetY)
        if (!facingRight) canvas.scale(-1f, 1f)

        when (state) {
            MascotState.DEAD -> {
                canvas.rotate(-90f)
                canvas.translate(-h * 0.15f, h * 0.55f)
            }
            MascotState.CROUCH -> canvas.scale(1f, 0.82f)
            MascotState.RUN -> canvas.rotate(7f)
            else -> Unit
        }

        val moving = (state == MascotState.WALK || state == MascotState.RUN)
        val phase = when (state) {
            MascotState.RUN -> animTick * 0.40f
            MascotState.WALK -> animTick * 0.25f
            else -> animTick * 0.06f
        }
        val swing = if (moving) sin(phase) else 0f
        val bob = if (moving) {
            -abs(sin(phase)) * h * 0.035f
        } else {
            sin(animTick * 0.05f) * h * 0.012f
        }
        val jitter = if (state == MascotState.PANIC || state == MascotState.GLITCH) {
            ((animTick % 5) - 2) * h * 0.02f
        } else 0f
        canvas.translate(jitter, bob)
        outline.strokeWidth = h * 0.02f

        val legLen = h * 0.30f
        val hipY = -legLen
        val torsoH = h * 0.36f
        val torsoW = h * 0.36f
        val shoulderY = hipY - torsoH + h * 0.06f
        val hoodH = h * 0.32f
        val hoodW = h * 0.40f
        val hoodTop = shoulderY - hoodH + h * 0.05f

        drawLeg(canvas, -swing, legLen, h, suitShade)
        drawArm(canvas, swing, shoulderY, h, suitShade, state, animTick)

        val torso = RectF(-torsoW / 2f, hipY - torsoH, torsoW / 2f, hipY + h * 0.02f)
        canvas.drawRoundRect(torso, h * 0.08f, h * 0.08f, suit)
        canvas.drawRoundRect(torso, h * 0.08f, h * 0.08f, outline)
        canvas.drawRect(RectF(-torsoW / 2f, hipY - h * 0.06f, torsoW / 2f, hipY - h * 0.02f), leather)

        drawLeg(canvas, swing, legLen, h, suit)

        val hood = RectF(-hoodW / 2f, hoodTop, hoodW / 2f, shoulderY + h * 0.06f)
        canvas.drawRoundRect(hood, h * 0.12f, h * 0.12f, suit)
        canvas.drawRoundRect(hood, h * 0.12f, h * 0.12f, outline)
        val visorRect = RectF(-hoodW * 0.10f, hoodTop + h * 0.06f, hoodW / 2f - h * 0.04f, hoodTop + h * 0.19f)
        canvas.drawRoundRect(visorRect, h * 0.06f, h * 0.06f, if (state == MascotState.DEAD) visorDead else visorPaint)
        canvas.drawRoundRect(
            RectF(visorRect.right - h * 0.09f, visorRect.top + h * 0.02f, visorRect.right - h * 0.04f, visorRect.top + h * 0.06f),
            h * 0.02f, h * 0.02f, visorGlint
        )

        drawArm(canvas, -swing, shoulderY, h, suit, state, animTick)

        if (state == MascotState.PANIC) {
            canvas.drawCircle(hoodW * 0.55f, hoodTop + h * 0.02f, h * 0.03f, sweat)
            canvas.drawCircle(hoodW * 0.70f, hoodTop + h * 0.10f, h * 0.022f, sweat)
        }
        if (state == MascotState.GLITCH) {
            val glitch = Paint().apply {
                color = if (animTick % 10 < 5) Color.parseColor("#66FF2BD9") else Color.parseColor("#662BD9FF")
            }
            val gy = hoodTop + (animTick % 7) * h * 0.04f
            canvas.drawRect(RectF(-hoodW / 2f, gy, hoodW / 2f, gy + h * 0.05f), glitch)
        }
        canvas.restore()
    }

    private fun drawLeg(canvas: Canvas, swing: Float, legLen: Float, h: Float, paint: Paint) {
        val footX = swing * h * 0.11f
        val lift = if (swing > 0f) swing * h * 0.045f else 0f
        val legW = h * 0.13f
        val leg = RectF(footX - legW / 2f, -legLen, footX + legW / 2f, -lift)
        canvas.drawRoundRect(leg, legW / 2f, legW / 2f, paint)
        canvas.drawRoundRect(leg, legW / 2f, legW / 2f, outline)
        val boot = RectF(footX - legW / 2f, -h * 0.09f - lift, footX + legW * 0.85f, -lift)
        canvas.drawRoundRect(boot, h * 0.035f, h * 0.035f, leather)
    }

    private fun drawArm(
        canvas: Canvas,
        swing: Float,
        shoulderY: Float,
        h: Float,
        paint: Paint,
        state: MascotState,
        animTick: Long
    ) {
        val armW = h * 0.10f
        val armLen = h * 0.24f
        if (state == MascotState.PANIC) {
            val wave = sin(animTick * 0.6f) * h * 0.05f
            val ax = h * 0.16f + wave
            val ay = shoulderY - h * 0.20f
            val arm = RectF(ax - armW / 2f, ay, ax + armW / 2f, shoulderY + h * 0.05f)
            canvas.drawRoundRect(arm, armW / 2f, armW / 2f, paint)
            canvas.drawCircle(ax, ay, h * 0.055f, leather)
            return
        }
        val handX = swing * h * 0.09f
        val arm = RectF(handX - armW / 2f, shoulderY, handX + armW / 2f, shoulderY + armLen)
        canvas.drawRoundRect(arm, armW / 2f, armW / 2f, paint)
        canvas.drawRoundRect(arm, armW / 2f, armW / 2f, outline)
        canvas.drawCircle(handX, shoulderY + armLen, h * 0.05f, leather)
    }
}
