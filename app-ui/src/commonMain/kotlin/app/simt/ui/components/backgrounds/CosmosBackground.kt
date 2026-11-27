package app.simt.ui.components.backgrounds

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import app.simt.ui.effects.CosmosTier
import app.simt.ui.effects.LocalCosmosTier
import app.simt.ui.effects.LocalUiMotionEnabled
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private val DefaultAccentColor = Color(0xFFFFC766)
private const val DEG_TO_RAD = (PI / 180.0).toFloat()

/** Flavor of the shared Cosmos background. Tunes star density and nebula presence. */
public enum class CosmosVariant {
    /** Main compass home — densest sky, nebula clouds, top glow. */
    Home,

    /** Standard screens and sheets — calm sky, no nebula. */
    Panel,

    /** Dialogs — sparse, dim sky so content stays dominant. */
    Dialog,

    /** Launch gates / permission moments — dense hero sky with nebula. */
    Gate,
}

/**
 * The shared warm deep-space background of the Cosmos design language.
 *
 * Full-tier motion adds nebula drift, ambient rotation and star twinkle. Reduced tier keeps
 * heading parallax without infinite animation. Static tier disables both ambient motion and
 * parallax. Disabled UI motion or [frozen] disables ambient animation; heading parallax remains
 * for FULL and REDUCED tiers.
 *
 * @param parallaxRotationDegrees Heading-driven sky rotation, read only in the draw phase.
 * @param accentColor Optional hue override for bright accent stars.
 * @param frozen Freezes ambient animation regardless of tier.
 */
@Composable
public fun CosmosBackground(
    modifier: Modifier = Modifier,
    variant: CosmosVariant = CosmosVariant.Panel,
    parallaxRotationDegrees: () -> Float = { 0f },
    accentColor: Color? = null,
    frozen: Boolean = false,
) {
    val tier = LocalCosmosTier.current
    val motionEnabled = LocalUiMotionEnabled.current
    val colorScheme = MaterialTheme.colorScheme
    val animated = motionEnabled && tier.allowsInfiniteAnimations && !frozen

    val stars =
        remember(variant, tier) {
            generateStars(variant, sparse = tier == CosmosTier.STATIC)
        }

    val isLightSky = remember(colorScheme.background) { colorScheme.background.luminance() > 0.5f }
    val palette =
        remember(colorScheme, accentColor, isLightSky) {
            CosmosPalette(
                voidCenter = colorScheme.background,
                voidEdge = lerp(colorScheme.background, colorScheme.tertiaryContainer, if (isLightSky) 0.30f else 0.22f),
                topGlow = colorScheme.primary.copy(alpha = if (isLightSky) 0.07f else 0.05f),
                star = if (isLightSky) colorScheme.onBackground.copy(alpha = 0.55f) else Color.White,
                accentA = accentColor ?: DefaultAccentColor,
                accentB = accentColor ?: colorScheme.tertiary,
                nebulaA = colorScheme.primary,
                nebulaB = colorScheme.tertiary,
            )
        }

    val infiniteTransition = if (animated) rememberInfiniteTransition(label = "cosmos") else null

    val twinkle =
        infiniteTransition?.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 2600, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "twinkle",
        )

    val drift =
        infiniteTransition?.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 96_000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "drift",
        )

    val showNebula = animated && (variant == CosmosVariant.Home || variant == CosmosVariant.Gate)
    val allowParallax = tier.allowsParallax

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val center = Offset(centerX, centerY)
        val maxRadius = maxOf(size.width, size.height)

        drawRect(
            brush =
                Brush.radialGradient(
                    colors = listOf(palette.voidCenter, palette.voidEdge),
                    center = center,
                    radius = maxRadius * 1.2f,
                ),
        )
        drawRect(
            brush =
                Brush.radialGradient(
                    colors = listOf(palette.topGlow, Color.Transparent),
                    center = Offset(centerX, 0f),
                    radius = maxRadius * 0.9f,
                ),
        )

        if (showNebula) {
            val driftRad = (drift?.value ?: 0f) * DEG_TO_RAD
            val orbitX = cos(driftRad) * size.width * 0.06f
            val orbitY = sin(driftRad) * size.height * 0.06f
            drawCircle(
                brush =
                    Brush.radialGradient(
                        colors = listOf(palette.nebulaA.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(centerX * 0.6f + orbitX, centerY * 0.7f + orbitY),
                        radius = maxRadius * 0.55f,
                    ),
                radius = maxRadius * 0.55f,
                center = Offset(centerX * 0.6f + orbitX, centerY * 0.7f + orbitY),
            )
            drawCircle(
                brush =
                    Brush.radialGradient(
                        colors = listOf(palette.nebulaB.copy(alpha = 0.06f), Color.Transparent),
                        center = Offset(centerX * 1.4f - orbitX, centerY * 1.25f - orbitY),
                        radius = maxRadius * 0.45f,
                    ),
                radius = maxRadius * 0.45f,
                center = Offset(centerX * 1.4f - orbitX, centerY * 1.25f - orbitY),
            )
        }

        val ambient = if (animated) (drift?.value ?: 0f) * 0.25f else 0f
        val rotation = (if (allowParallax) parallaxRotationDegrees() else 0f) + ambient
        val twinklePhase = twinkle?.value ?: 0.5f
        rotate(degrees = rotation, pivot = center) {
            drawStars(stars, palette, center, maxRadius, twinklePhase, animated)
        }
    }
}

private class CosmosPalette(
    val voidCenter: Color,
    val voidEdge: Color,
    val topGlow: Color,
    val star: Color,
    val accentA: Color,
    val accentB: Color,
    val nebulaA: Color,
    val nebulaB: Color,
)

private class CosmosStar(
    val cosAngle: Float,
    val sinAngle: Float,
    val radiusFraction: Float,
    val sizeFactor: Float,
    val alpha: Float,
    val kind: Int,
    val twinkleOffset: Float,
)

private fun generateStars(
    variant: CosmosVariant,
    sparse: Boolean,
): List<CosmosStar> {
    val baseCount =
        when (variant) {
            CosmosVariant.Home -> 140
            CosmosVariant.Panel -> 90
            CosmosVariant.Dialog -> 64
            CosmosVariant.Gate -> 120
        }
    val count = if (sparse) (baseCount * 0.4f).toInt() else baseCount
    val random = Random(42 + variant.ordinal)
    return List(count) { i ->
        val angle = random.nextFloat() * 360f * DEG_TO_RAD
        val radiusFraction = sqrt(random.nextFloat()) * 0.78f
        val kind =
            when {
                i % 10 == 0 -> 2
                i % 3 == 0 -> 1
                else -> 0
            }
        CosmosStar(
            cosAngle = cos(angle),
            sinAngle = sin(angle),
            radiusFraction = radiusFraction,
            sizeFactor =
                when (kind) {
                    2 -> 1.6f + random.nextFloat() * 0.8f
                    1 -> 1.0f + random.nextFloat() * 0.5f
                    else -> 0.5f + random.nextFloat() * 0.4f
                },
            alpha =
                when (kind) {
                    2 -> 0.95f
                    1 -> 0.55f
                    else -> 0.34f
                },
            kind = kind,
            twinkleOffset = random.nextFloat(),
        )
    }
}

private fun DrawScope.drawStars(
    stars: List<CosmosStar>,
    palette: CosmosPalette,
    center: Offset,
    maxRadius: Float,
    twinklePhase: Float,
    animated: Boolean,
) {
    val dustRadius = 1.dp.toPx()
    for (index in stars.indices) {
        val star = stars[index]
        val distance = star.radiusFraction * maxRadius
        val x = center.x + distance * star.cosAngle
        val y = center.y + distance * star.sinAngle
        val color =
            when (star.kind) {
                2 -> if (index % 2 == 0) palette.accentA else palette.accentB
                else -> palette.star
            }
        val alpha =
            if (animated && star.kind == 2) {
                val phase = (twinklePhase + star.twinkleOffset) % 1f
                star.alpha * (0.55f + 0.45f * phase)
            } else {
                star.alpha
            }
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = dustRadius * star.sizeFactor,
            center = Offset(x, y),
        )
    }
}
