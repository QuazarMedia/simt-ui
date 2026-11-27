package app.simt.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.simt.ui.components.backgrounds.CosmosSpecks
import app.simt.ui.effects.LocalCosmosTier
import app.simt.ui.effects.LocalUiMotionEnabled
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Expanding search waves with a caller-provided decorative icon. */
@Composable
public fun CosmosWaveField(
    accent: Color,
    secondary: Color,
    active: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val tier = LocalCosmosTier.current
    val motionEnabled = LocalUiMotionEnabled.current
    val infiniteMotion = motionEnabled && tier.allowsInfiniteAnimations
    val dominant = if (active) secondary else accent
    val surface = MaterialTheme.colorScheme.surface

    Box(modifier = modifier.size(150.dp), contentAlignment = Alignment.Center) {
        CosmosSpecks(colors = listOf(accent, secondary), modifier = Modifier.fillMaxSize())

        if (infiniteMotion) {
            val transition = rememberInfiniteTransition(label = "CosmosWaves")
            val progress by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(if (active) 1500 else 2300, easing = LinearEasing)),
                label = "WaveProgress",
            )
            val spin by transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(if (active) 3800 else 6200, easing = LinearEasing)),
                label = "WaveSpin",
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                val maxR = size.minDimension / 2f
                val rings = 4
                for (i in 0 until rings) {
                    val p = (progress + i.toFloat() / rings) % 1f
                    drawCircle(
                        color = (if (i % 2 == 0) accent else secondary).copy(alpha = (1f - p) * 0.5f),
                        radius = maxR * (0.25f + p * 0.72f),
                        style = Stroke(width = 3f),
                    )
                }
                val dash = PathEffect.dashPathEffect(floatArrayOf(5f, 12f))
                rotate(spin) {
                    drawCircle(
                        color = dominant.copy(alpha = 0.35f),
                        radius = maxR * 0.92f,
                        style = Stroke(width = 2f, pathEffect = dash),
                    )
                }
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val maxR = size.minDimension / 2f
                listOf(0.45f, 0.7f, 0.92f).forEachIndexed { i, fraction ->
                    drawCircle(
                        color = (if (i % 2 == 0) accent else secondary).copy(alpha = 0.28f),
                        radius = maxR * fraction,
                        style = Stroke(width = 2.5f),
                    )
                }
            }
        }

        val coreScale =
            if (infiniteMotion) {
                val transition = rememberInfiniteTransition(label = "CosmosCore")
                val scale by transition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.08f,
                    animationSpec = infiniteRepeatable(tween(900), repeatMode = RepeatMode.Reverse),
                    label = "CoreScale",
                )
                scale
            } else {
                1f
            }

        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(64.dp)
                    .scale(coreScale)
                    .background(
                        brush =
                            Brush.radialGradient(
                                listOf(dominant.copy(alpha = 0.35f), dominant.copy(alpha = 0.10f), Color.Transparent),
                            ),
                        shape = CircleShape,
                    ),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(surface.copy(alpha = 0.7f))
                        .border(1.dp, dominant.copy(alpha = 0.5f), CircleShape),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = dominant,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        if (active) {
            CircularProgressIndicator(
                modifier = Modifier.size(74.dp),
                strokeWidth = 2.dp,
                color = secondary.copy(alpha = 0.8f),
            )
        }
    }
}

/** A springing check orb with an expanding ring and outward sparks. */
@Composable
public fun CosmosSuccessBurst(
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val tier = LocalCosmosTier.current
    val motionEnabled = LocalUiMotionEnabled.current
    val entranceMotion = motionEnabled && tier.allowsEntranceAnimations
    val infiniteMotion = motionEnabled && tier.allowsInfiniteAnimations
    val burst = remember { Animatable(if (entranceMotion) 0f else 1f) }
    val check = remember { Animatable(if (entranceMotion) 0f else 1f) }
    LaunchedEffect(entranceMotion) {
        if (entranceMotion) {
            burst.animateTo(1f, animationSpec = tween(760, easing = FastOutSlowInEasing))
        } else {
            burst.snapTo(1f)
        }
    }
    LaunchedEffect(entranceMotion) {
        if (entranceMotion) {
            check.animateTo(1f, animationSpec = spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMedium))
        } else {
            check.snapTo(1f)
        }
    }
    val glow =
        if (infiniteMotion) {
            val transition = rememberInfiniteTransition(label = "SuccessGlow")
            val scale by transition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(tween(1200), repeatMode = RepeatMode.Reverse),
                label = "Glow",
            )
            scale
        } else {
            1f
        }

    Box(modifier = modifier.size(150.dp), contentAlignment = Alignment.Center) {
        CosmosSpecks(colors = listOf(accent, accent), modifier = Modifier.fillMaxSize())
        Canvas(modifier = Modifier.fillMaxSize()) {
            val maxR = size.minDimension / 2f
            val progress = burst.value
            if (progress < 1f) {
                drawCircle(
                    color = accent.copy(alpha = (1f - progress) * 0.5f),
                    radius = maxR * (0.3f + progress * 0.7f),
                    style = Stroke(width = 4f),
                )
            }
            val particles = 8
            for (i in 0 until particles) {
                val angle = i * 2.0 * PI / particles
                val distance = maxR * (0.35f + progress * 0.55f)
                drawCircle(
                    color = accent.copy(alpha = (1f - progress) * 0.8f),
                    radius = 3f,
                    center = center + Offset((cos(angle) * distance).toFloat(), (sin(angle) * distance).toFloat()),
                )
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(72.dp)
                    .scale(check.value * glow)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.95f), accent.copy(alpha = 0.6f)))),
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

/** The error counterpart to [CosmosSuccessBurst], with a brief side-to-side shake. */
@Composable
public fun CosmosErrorBurst(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.error,
) {
    val tier = LocalCosmosTier.current
    val motionEnabled = LocalUiMotionEnabled.current
    val entranceMotion = motionEnabled && tier.allowsEntranceAnimations
    val infiniteMotion = motionEnabled && tier.allowsInfiniteAnimations
    val burst = remember { Animatable(if (entranceMotion) 0f else 1f) }
    val mark = remember { Animatable(if (entranceMotion) 0f else 1f) }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(entranceMotion) {
        if (entranceMotion) {
            burst.animateTo(1f, animationSpec = tween(760, easing = FastOutSlowInEasing))
        } else {
            burst.snapTo(1f)
        }
    }
    LaunchedEffect(entranceMotion) {
        if (entranceMotion) {
            mark.animateTo(1f, animationSpec = spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMedium))
        } else {
            mark.snapTo(1f)
        }
    }
    LaunchedEffect(entranceMotion) {
        if (entranceMotion) {
            shake.animateTo(
                targetValue = 0f,
                animationSpec =
                    keyframes {
                        durationMillis = 440
                        0f at 0
                        -1f at 70
                        1f at 150
                        -0.6f at 240
                        0.3f at 340
                        0f at 440
                    },
            )
        } else {
            shake.snapTo(0f)
        }
    }
    val glow =
        if (infiniteMotion) {
            val transition = rememberInfiniteTransition(label = "ErrorGlow")
            val scale by transition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(tween(1200), repeatMode = RepeatMode.Reverse),
                label = "Glow",
            )
            scale
        } else {
            1f
        }

    Box(modifier = modifier.size(150.dp), contentAlignment = Alignment.Center) {
        CosmosSpecks(colors = listOf(color, color), modifier = Modifier.fillMaxSize())
        Canvas(modifier = Modifier.fillMaxSize()) {
            val maxR = size.minDimension / 2f
            val progress = burst.value
            if (progress < 1f) {
                drawCircle(
                    color = color.copy(alpha = (1f - progress) * 0.5f),
                    radius = maxR * (0.3f + progress * 0.7f),
                    style = Stroke(width = 4f),
                )
            }
            val particles = 8
            for (i in 0 until particles) {
                val angle = i * 2.0 * PI / particles
                val distance = maxR * (0.35f + progress * 0.55f)
                drawCircle(
                    color = color.copy(alpha = (1f - progress) * 0.8f),
                    radius = 3f,
                    center = center + Offset((cos(angle) * distance).toFloat(), (sin(angle) * distance).toFloat()),
                )
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(72.dp)
                    .graphicsLayer {
                        scaleX = mark.value * glow
                        scaleY = mark.value * glow
                        translationX = shake.value * 6.dp.toPx()
                    }
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(color.copy(alpha = 0.95f), color.copy(alpha = 0.6f)))),
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}
