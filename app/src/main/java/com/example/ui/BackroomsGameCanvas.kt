package com.example.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import com.example.R
import com.example.game.BackroomsGameRenderer
import com.example.model.DayStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun BackroomsGameCanvas(
    dayStats: DayStats,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val wireframeBitmap = remember {
        try {
            BitmapFactory.decodeResource(context.resources, R.drawable.wireframe_entity)
        } catch (e: Exception) {
            null
        }
    }
    val renderer = remember(wireframeBitmap) {
        BackroomsGameRenderer(wireframeBitmap = wireframeBitmap)
    }
    var animTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (isActive) {
            animTick++
            delay(16L)
        }
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        val nativeCanvas = drawContext.canvas.nativeCanvas
        renderer.render(
            canvas = nativeCanvas,
            viewWidth = size.width,
            viewHeight = size.height,
            dayStats = dayStats,
            animTick = animTick
        )
    }
}
