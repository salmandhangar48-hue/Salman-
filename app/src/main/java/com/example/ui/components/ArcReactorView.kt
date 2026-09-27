package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SalmanVoiceState
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisBlueArc
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanGlow
import com.example.ui.theme.JarvisRedAlert
import com.example.ui.theme.JarvisTeal
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorView(
    state: SalmanVoiceState,
    amplitude: Float,
    modifier: Modifier = Modifier,
    reactorSize: Dp = 240.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ReactorTransition")

    // Dynamic rotation speeds: accelerates during PROCESSING (thinking)
    val rotationDuration = when (state) {
        SalmanVoiceState.PROCESSING -> 2400
        SalmanVoiceState.LISTENING -> 4500
        SalmanVoiceState.SPEAKING -> 3500
        else -> 6500
    }

    val rotationFast by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = rotationDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "FastRotation"
    )

    val rotationReverse by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (rotationDuration * 1.5f).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ReverseRotation"
    )

    // 1. Idle subtle breathing pulse
    val idleBreathingScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "IdleBreathing"
    )

    // 2. Listening Pulse: Smooth expansive scaling & opacity oscillation
    val listeningPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ListeningPulseScale"
    )

    val listeningPulseOpacity by infiniteTransition.animateFloat(
        initialValue = 0.50f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ListeningPulseOpacity"
    )

    // Sonar radar wave radiating outwards when listening
    val listeningWaveRadiusProgress by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ListeningWaveRadius"
    )

    // 3. Thinking (PROCESSING) Pulse: Rapid cognitive rhythmic scaling & opacity oscillation
    val thinkingPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ThinkingPulseScale"
    )

    val thinkingPulseOpacity by infiniteTransition.animateFloat(
        initialValue = 0.38f,
        targetValue = 0.98f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ThinkingPulseOpacity"
    )

    // Neural ripple waves radiating outwards when thinking
    val thinkingWaveProgress by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ThinkingWaveProgress"
    )

    // Determine target scale and opacity based on active state
    val targetScale = when (state) {
        SalmanVoiceState.LISTENING -> listeningPulseScale + (amplitude * 0.22f)
        SalmanVoiceState.PROCESSING -> thinkingPulseScale
        SalmanVoiceState.SPEAKING -> 1.0f + (amplitude * 0.32f)
        SalmanVoiceState.ERROR -> 1.0f
        SalmanVoiceState.IDLE -> idleBreathingScale
    }

    val targetCoreOpacity = when (state) {
        SalmanVoiceState.LISTENING -> listeningPulseOpacity
        SalmanVoiceState.PROCESSING -> thinkingPulseOpacity
        SalmanVoiceState.SPEAKING -> (0.7f + amplitude * 0.3f).coerceIn(0.7f, 1.0f)
        SalmanVoiceState.ERROR -> 0.85f
        SalmanVoiceState.IDLE -> 0.70f
    }

    // Smoothly animate between state transitions
    val animatedOverallScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "OverallScaleAnim"
    )

    val animatedCoreOpacity by animateFloatAsState(
        targetValue = targetCoreOpacity,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "CoreOpacityAnim"
    )

    val targetPrimaryColor = when (state) {
        SalmanVoiceState.LISTENING -> JarvisTeal
        SalmanVoiceState.SPEAKING -> JarvisCyan
        SalmanVoiceState.PROCESSING -> JarvisAmber
        SalmanVoiceState.ERROR -> JarvisRedAlert
        SalmanVoiceState.IDLE -> JarvisBlueArc
    }

    val targetSecondaryColor = when (state) {
        SalmanVoiceState.LISTENING -> JarvisCyan
        SalmanVoiceState.SPEAKING -> JarvisBlueArc
        SalmanVoiceState.PROCESSING -> JarvisTeal
        SalmanVoiceState.ERROR -> JarvisAmber
        SalmanVoiceState.IDLE -> JarvisCyanGlow
    }

    val primaryColor by animateColorAsState(
        targetValue = targetPrimaryColor,
        animationSpec = tween(durationMillis = 350),
        label = "PrimaryColorAnim"
    )

    val secondaryColor by animateColorAsState(
        targetValue = targetSecondaryColor,
        animationSpec = tween(durationMillis = 350),
        label = "SecondaryColorAnim"
    )

    Box(
        modifier = modifier.size(reactorSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = (minOf(size.width, size.height) / 2f) * 0.95f

            // 1. Dynamic outer pulse wave for LISTENING or PROCESSING states
            if (state == SalmanVoiceState.LISTENING) {
                val waveRadius = maxRadius * listeningWaveRadiusProgress
                val waveAlpha = ((1.05f - listeningWaveRadiusProgress) * 0.75f).coerceIn(0f, 0.75f)
                drawCircle(
                    color = JarvisTeal.copy(alpha = waveAlpha),
                    radius = waveRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            } else if (state == SalmanVoiceState.PROCESSING) {
                val waveRadius = maxRadius * thinkingWaveProgress
                val waveAlpha = ((0.95f - thinkingWaveProgress) * 0.85f).coerceIn(0f, 0.85f)
                drawCircle(
                    color = JarvisAmber.copy(alpha = waveAlpha),
                    radius = waveRadius,
                    center = center,
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }

            // 2. Background radial glow with state-driven animated opacity and scale
            val glowRadius = maxRadius * animatedOverallScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.45f * animatedCoreOpacity),
                        secondaryColor.copy(alpha = 0.18f * animatedCoreOpacity),
                        Color.Transparent
                    ),
                    center = center,
                    radius = glowRadius.coerceAtLeast(10f)
                ),
                radius = glowRadius,
                center = center
            )

            // 3. Outer telemetry ring with tick marks
            drawCircle(
                color = primaryColor.copy(alpha = 0.45f * animatedCoreOpacity),
                radius = maxRadius * 0.92f,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 4. Compass / HUD ticks (36 ticks around outer perimeter)
            val numTicks = 36
            val tickRadiusInner = maxRadius * 0.86f
            val tickRadiusOuter = maxRadius * 0.91f
            for (i in 0 until numTicks) {
                val angleRad = Math.toRadians((i * (360f / numTicks)).toDouble())
                val start = Offset(
                    center.x + (tickRadiusInner * cos(angleRad)).toFloat(),
                    center.y + (tickRadiusInner * sin(angleRad)).toFloat()
                )
                val isMajorTick = i % 3 == 0
                val outerRad = if (isMajorTick) tickRadiusOuter else tickRadiusOuter * 0.98f
                val end = Offset(
                    center.x + (outerRad * cos(angleRad)).toFloat(),
                    center.y + (outerRad * sin(angleRad)).toFloat()
                )
                drawLine(
                    color = if (isMajorTick) primaryColor.copy(alpha = 0.85f * animatedCoreOpacity)
                    else secondaryColor.copy(alpha = 0.4f * animatedCoreOpacity),
                    start = start,
                    end = end,
                    strokeWidth = if (isMajorTick) 2.5.dp.toPx() else 1.dp.toPx()
                )
            }

            // 5. Segmented rotating outer gear (Arcs clockwise)
            rotate(rotationFast, pivot = center) {
                val outerArcRadius = maxRadius * 0.78f
                val arcStroke = 3.5.dp.toPx()
                val arcSize = Size(outerArcRadius * 2, outerArcRadius * 2)
                val arcTopLeft = Offset(center.x - outerArcRadius, center.y - outerArcRadius)

                // 4 prominent segmented arcs
                for (arcIndex in 0 until 4) {
                    val startAngle = arcIndex * 90f + 10f
                    drawArc(
                        color = primaryColor.copy(alpha = 0.85f * animatedCoreOpacity),
                        startAngle = startAngle,
                        sweepAngle = 70f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = arcStroke, cap = StrokeCap.Round)
                    )
                }
            }

            // 6. Segmented rotating inner gear (Arcs counter-clockwise)
            rotate(rotationReverse, pivot = center) {
                val midArcRadius = maxRadius * 0.62f
                val midStroke = 4.dp.toPx()
                val midSize = Size(midArcRadius * 2, midArcRadius * 2)
                val midTopLeft = Offset(center.x - midArcRadius, center.y - midArcRadius)

                for (arcIndex in 0 until 6) {
                    val startAngle = arcIndex * 60f + 8f
                    drawArc(
                        color = secondaryColor.copy(alpha = 0.9f * animatedCoreOpacity),
                        startAngle = startAngle,
                        sweepAngle = 44f,
                        useCenter = false,
                        topLeft = midTopLeft,
                        size = midSize,
                        style = Stroke(width = midStroke, cap = StrokeCap.Round)
                    )
                }
            }

            // 7. Inner Core Rings
            val innerRingRadius = maxRadius * 0.44f * animatedOverallScale
            drawCircle(
                color = primaryColor.copy(alpha = 0.75f * animatedCoreOpacity),
                radius = innerRingRadius,
                style = Stroke(width = 2.dp.toPx())
            )

            // Inner triangular crosshair lines reacting to scale
            val coreRadius = maxRadius * 0.28f * animatedOverallScale
            for (i in 0 until 3) {
                val angleRad = Math.toRadians((rotationFast * 0.5f + i * 120f).toDouble())
                val p1 = Offset(
                    center.x + (coreRadius * cos(angleRad)).toFloat(),
                    center.y + (coreRadius * sin(angleRad)).toFloat()
                )
                val p2 = Offset(
                    center.x - (coreRadius * cos(angleRad)).toFloat(),
                    center.y - (coreRadius * sin(angleRad)).toFloat()
                )
                drawLine(
                    color = primaryColor.copy(alpha = 0.65f * animatedCoreOpacity),
                    start = p1,
                    end = p2,
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            // 8. Glowing Central Arc Reactor Core Center with smooth scaling and opacity pulse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = animatedCoreOpacity),
                        primaryColor.copy(alpha = animatedCoreOpacity),
                        primaryColor.copy(alpha = 0.7f * animatedCoreOpacity),
                        Color.Transparent
                    ),
                    center = center,
                    radius = (coreRadius * 1.35f).coerceAtLeast(5f)
                ),
                radius = coreRadius,
                center = center
            )
        }

        // Center HUD Emblem Label with pulsing opacity and dynamic indicator
        Text(
            text = if (state == SalmanVoiceState.PROCESSING) "THINKING" else "SALMAN",
            color = Color.White.copy(alpha = animatedCoreOpacity.coerceIn(0.6f, 1.0f)),
            fontSize = if (state == SalmanVoiceState.PROCESSING) 8.5.sp else 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = if (state == SalmanVoiceState.PROCESSING) 1.5.sp else 2.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 1.dp)
        )
    }
}
