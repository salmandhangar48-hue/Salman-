package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.SalmanVoiceState
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisBlueArc
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisTeal
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun AudioWaveformBar(
    state: SalmanVoiceState,
    amplitude: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 24,
    height: Dp = 48.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveformAnim")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    val activeColor = when (state) {
        SalmanVoiceState.LISTENING -> JarvisTeal
        SalmanVoiceState.SPEAKING -> JarvisCyan
        SalmanVoiceState.PROCESSING -> JarvisAmber
        SalmanVoiceState.ERROR -> Color(0xFFFF3366)
        SalmanVoiceState.IDLE -> JarvisBlueArc.copy(alpha = 0.6f)
    }

    val gradientColors = listOf(
        activeColor,
        JarvisBlueArc,
        activeColor.copy(alpha = 0.4f)
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val totalWidth = size.width
        val canvasHeight = size.height
        val barWidth = (totalWidth / (barCount * 1.6f)).coerceIn(3.dp.toPx(), 12.dp.toPx())
        val spacing = (totalWidth - (barCount * barWidth)) / (barCount + 1)
        val centerY = canvasHeight / 2f

        for (i in 0 until barCount) {
            val normalizedIndex = i.toFloat() / barCount
            // Bell curve dampening at edges
            val envelope = sin(normalizedIndex * Math.PI.toFloat())

            // Sinusoidal oscillation
            val wave = abs(sin(phase + (i * 0.45f)))

            val effectiveAmp = when (state) {
                SalmanVoiceState.SPEAKING -> (amplitude * 0.7f + wave * 0.3f).coerceIn(0.15f, 1.0f)
                SalmanVoiceState.LISTENING -> (amplitude * 0.8f + wave * 0.2f).coerceIn(0.12f, 1.0f)
                SalmanVoiceState.PROCESSING -> (0.25f + wave * 0.45f)
                SalmanVoiceState.IDLE -> 0.08f + (wave * 0.07f)
                SalmanVoiceState.ERROR -> 0.1f
            }

            val barHeight = (canvasHeight * 0.85f * effectiveAmp * envelope).coerceAtLeast(3.dp.toPx())
            val left = spacing + i * (barWidth + spacing)
            val top = centerY - (barHeight / 2f)

            // Draw rounded bar
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = gradientColors,
                    startY = top,
                    endY = top + barHeight
                ),
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )

            // Draw glowing peak dot
            if (state == SalmanVoiceState.SPEAKING || state == SalmanVoiceState.LISTENING) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = (barWidth * 0.45f),
                    center = Offset(left + barWidth / 2f, top - 4.dp.toPx())
                )
            }
        }
    }
}
