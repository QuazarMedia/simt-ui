package app.simt.ui.components.dialogs

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.simt.ui.effects.LocalUiMotionEnabled
import app.simt.ui.resources.Res
import app.simt.ui.resources.simt_ui_update_available_cancel
import app.simt.ui.resources.simt_ui_update_available_description_fmt
import app.simt.ui.resources.simt_ui_update_available_title_fmt
import app.simt.ui.resources.simt_ui_update_available_update
import app.simt.ui.resources.simt_ui_update_available_version_fmt
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val TWO_PI_F = (2.0 * PI).toFloat()
private const val RING_DASH_COUNT = 12
private const val RING_ANGLE_STEP_DEG = 360f / RING_DASH_COUNT
private const val DEG_TO_RAD = (PI / 180.0).toFloat()
private const val ICON_SIZE_DP = 48

/**
 * Particle descriptor for the data-stream animation in [UpdateDialogBanner].
 */
private data class StreamParticle(
    val xFraction: Float,
    val speedFraction: Float,
    val size: Float,
    val alpha: Float,
    val phaseOffset: Float,
)

/**
 * A visually rich, self-contained dialog shown when an app update is available.
 *
 * Renders an animated gradient header banner with a pulsing update icon, the app name and
 * current version, and a cancel/update action row. Colours come from the caller's Material3
 * theme. The banner honors [LocalUiMotionEnabled]: with motion disabled it renders a static
 * gradient and centred icon instead of the particle and ring animations.
 *
 * [appName] is interpolated into the title and description so a single dialog instance
 * adapts to whichever application launches it.
 *
 * Dismissal is owned by the caller: [onDismiss] fires for the cancel button, the back press
 * and clicks outside the card alike; the dialog does not remove itself.
 *
 * @param appName Display name of the application, embedded in the title and description.
 * @param currentVersion The version currently installed, shown in the body.
 * @param onUpdate Invoked when the user taps the update action.
 * @param onDismiss Invoked when the user dismisses the dialog.
 * @param modifier Optional [Modifier] applied to the card.
 */
@Composable
public fun UpdateAvailableDialog(
    appName: String,
    currentVersion: String,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            ),
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = modifier,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                UpdateDialogBanner(icon = Icons.Default.SystemUpdateAlt)

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(Res.string.simt_ui_update_available_title_fmt, appName),
                    style =
                        MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(Res.string.simt_ui_update_available_version_fmt, currentVersion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(Res.string.simt_ui_update_available_description_fmt, appName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("update_dialog_cancel").heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(Res.string.simt_ui_update_available_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onUpdate,
                        modifier = Modifier.testTag("update_dialog_update").heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdateAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(Res.string.simt_ui_update_available_update))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

/**
 * Animated header banner for [UpdateAvailableDialog].
 *
 * Renders a gradient background with floating data-stream particles that drift upward, a
 * slowly rotating orbital ring, and a central pulsing icon. With [LocalUiMotionEnabled]
 * disabled it degrades to a static gradient with the centred icon.
 *
 * ### Performance notes
 * - Animated values (`time`, `ringAngle`, `pulseState`) are only read inside the [Canvas] draw
 *   lambda or a [graphicsLayer] block, so changes invalidate the **draw phase** only — no
 *   recomposition.
 * - The gradient [Brush] and clip [RoundedCornerShape] are cached via `remember`.
 */
@Composable
private fun UpdateDialogBanner(
    icon: ImageVector,
    height: Dp = 160.dp,
    cornerRadius: Dp = 24.dp,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val accentColor = MaterialTheme.colorScheme.tertiary
    val contentColor = MaterialTheme.colorScheme.onPrimary

    // Motion disabled: static gradient + centred icon, no particles or ring animation.
    if (!LocalUiMotionEnabled.current) {
        val gradientBrush =
            remember(primaryColor, accentColor) {
                Brush.verticalGradient(
                    colors =
                        listOf(
                            primaryColor.copy(alpha = 0.85f),
                            accentColor.copy(alpha = 0.6f),
                            primaryColor.copy(alpha = 0.95f),
                        ),
                )
            }
        val clipShape =
            remember(cornerRadius) {
                RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
            }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    .clip(clipShape)
                    .background(brush = gradientBrush),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(ICON_SIZE_DP.dp),
            )
        }
        return
    }

    // Generate stable random-looking particles (computed once).
    val particleCount = 28
    val particles =
        remember(particleCount) {
            List(particleCount) { i ->
                StreamParticle(
                    xFraction = ((i * 37 + 13) % 100) / 100f,
                    speedFraction = 0.3f + ((i * 53 + 7) % 70) / 100f,
                    size = 2f + ((i * 29 + 3) % 40) / 10f,
                    alpha = 0.15f + ((i * 41 + 11) % 50) / 100f,
                    phaseOffset = ((i * 67 + 19) % 100) / 100f,
                )
            }
        }

    // Cache gradient brush — only re-created when colours change, not every frame.
    val gradientBrush =
        remember(primaryColor, accentColor) {
            Brush.verticalGradient(
                colors =
                    listOf(
                        primaryColor.copy(alpha = 0.85f),
                        accentColor.copy(alpha = 0.6f),
                        primaryColor.copy(alpha = 0.95f),
                    ),
            )
        }

    // Cache clip shape.
    val clipShape =
        remember(cornerRadius) {
            RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
        }

    val infiniteTransition = rememberInfiniteTransition(label = "update_dialog_banner")

    // Time drives the upward particle drift — read only in Canvas (draw-phase).
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 6000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "time",
    )

    // Pulsing scale — kept as State so it is read only inside graphicsLayer (draw-phase).
    val pulseState =
        infiniteTransition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "pulse",
        )

    // Rotating ring angle — read only in Canvas (draw-phase).
    val ringAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 8000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "ring",
    )

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(height)
                .clip(clipShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(height),
        ) {
            val w = size.width
            val h = size.height

            // Gradient background (brush is cached, shader created once per size).
            drawRect(brush = gradientBrush)

            // Floating data-stream particles drifting upward.
            for (idx in particles.indices) {
                val p = particles[idx]
                val progress = (time + p.phaseOffset) % 1f
                val y = h * (1f - progress * 1.2f)
                val xWobble = sin((progress + p.phaseOffset) * TWO_PI_F) * w * 0.03f
                val x = p.xFraction * w + xWobble

                // Fade in/out near edges.
                val fadeAlpha =
                    when {
                        progress < 0.1f -> progress * 10f
                        progress > 0.85f -> (1f - progress) * 6.6667f
                        else -> 1f
                    }.coerceIn(0f, 1f) * p.alpha

                drawCircle(
                    color = contentColor.copy(alpha = fadeAlpha),
                    radius = p.size,
                    center = Offset(x, y),
                )
            }

            // Rotating dashed orbital ring.
            val cx = w / 2f
            val cy = h / 2f
            val ringRadius = minOf(w, h) * 0.30f
            for (i in 0 until RING_DASH_COUNT) {
                val angle = (ringAngle + i * RING_ANGLE_STEP_DEG) * DEG_TO_RAD
                val dx = cos(angle) * ringRadius
                val dy = sin(angle) * ringRadius
                val dashAlpha = if (i % 3 == 0) 0.5f else 0.2f
                drawCircle(
                    color = contentColor.copy(alpha = dashAlpha),
                    radius = 2.5f,
                    center = Offset(cx + dx, cy + dy),
                )
            }
        }

        // Central icon — pulse applied via graphicsLayer (skips composition & layout).
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier =
                Modifier
                    .size(ICON_SIZE_DP.dp)
                    .graphicsLayer {
                        val s = pulseState.value
                        scaleX = s
                        scaleY = s
                    },
        )
    }
}
