package com.personx.hermatic.ui.components

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import com.personx.hermatic.ui.theme.NousBlack
import kotlin.math.roundToInt
import kotlin.random.Random

private data class NoiseDot(val x: Float, val y: Float, val alpha: Float, val sizeDp: Float)

@Composable
fun NoisyAmbientBackground(primaryColor: Color, accentColor: Color) {
    val densityObj = LocalDensity.current

    val noiseDots = remember {
        List(50) {
            NoiseDot(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                alpha = Random.nextFloat() * 0.05f + 0.01f,
                sizeDp = Random.nextFloat() * 0.7f + 0.5f
            )
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(NousBlack)
    ) {
        val widthPx = with(densityObj) { maxWidth.toPx().roundToInt() }.coerceAtLeast(1)
        val heightPx = with(densityObj) { maxHeight.toPx().roundToInt() }.coerceAtLeast(1)
        val dpScale = densityObj.density

        val cachedBitmap = remember(primaryColor, accentColor, widthPx, heightPx) {
            if (widthPx <= 0 || heightPx <= 0) return@remember null

            val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            val w = widthPx.toFloat()
            val h = heightPx.toFloat()

            val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG)

            fun drawRadial(cx: Float, cy: Float, r: Float, color: Color) {
                gradientPaint.shader = RadialGradient(
                    cx, cy, r,
                    color.toArgb(),
                    android.graphics.Color.TRANSPARENT,
                    Shader.TileMode.CLAMP
                )
                canvas.drawCircle(cx, cy, r, gradientPaint)
            }

            drawRadial(w * 0.8f, h * 0.1f, w * 0.8f, primaryColor.copy(alpha = 0.15f))
            drawRadial(0f, h * 0.5f, w * 0.9f, accentColor.copy(alpha = 0.12f))
            drawRadial(w * 0.5f, h * 0.9f, w * 0.7f, primaryColor.copy(alpha = 0.1f))

            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
            noiseDots.forEach { dot ->
                dotPaint.alpha = (dot.alpha * 255).roundToInt().coerceIn(0, 255)
                canvas.drawCircle(dot.x * w, dot.y * h, dot.sizeDp * dpScale, dotPaint)
            }

            bmp.asImageBitmap()
        }

        if (cachedBitmap != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawImage(cachedBitmap, Offset.Zero)
            }
        }
    }
}
