package app.simt.ui.components.backgrounds

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import app.simt.ui.effects.LocalCosmosTier
import app.simt.ui.effects.LocalUiMotionEnabled
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Speck(
    val angle: Float,
    val radiusFraction: Float,
    val radiusPx: Float,
    val phase: Float,
    val speed: Float,
)

/** A deterministic field of faint stars that twinkles only when full-tier motion is enabled. */
@Composable
public fun CosmosSpecks(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    count: Int = 22,
) {
    require(count >= 0) { "count must not be negative" }
    require(count == 0 || colors.isNotEmpty()) { "colors must not be empty when count is positive" }

    val motionEnabled = LocalUiMotionEnabled.current && LocalCosmosTier.current.allowsInfiniteAnimations
    val specks =
        remember(count) {
            val rng = Random(count * 31 + 7)
            List(count) {
                Speck(
                    angle = rng.nextFloat() * 360f,
                    radiusFraction = 0.30f + rng.nextFloat() * 0.68f,
                    radiusPx = 1.2f + rng.nextFloat() * 2.4f,
                    phase = rng.nextFloat(),
                    speed = 0.6f + rng.nextFloat() * 0.9f,
                )
            }
        }

    val twinkle =
        if (motionEnabled) {
            val transition = rememberInfiniteTransition(label = "CosmosSpecks")
            val t by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(4200), repeatMode = RepeatMode.Reverse),
                label = "SpeckTwinkle",
            )
            t
        } else {
            0.6f
        }

    Canvas(modifier = modifier.fillMaxSize()) {
        val maxR = size.minDimension / 2f
        specks.forEachIndexed { index, speck ->
            val radians = speck.angle.toDouble() * PI / 180.0
            val position =
                center +
                    Offset(
                        (cos(radians) * maxR * speck.radiusFraction).toFloat(),
                        (sin(radians) * maxR * speck.radiusFraction).toFloat(),
                    )
            val wave = sin((twinkle * speck.speed + speck.phase) * 2f * PI.toFloat())
            val alpha = 0.18f + 0.55f * (wave * 0.5f + 0.5f)
            drawCircle(
                color = colors[index % colors.size].copy(alpha = alpha),
                radius = speck.radiusPx,
                center = position,
            )
        }
    }
}
