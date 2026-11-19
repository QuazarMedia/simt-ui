package app.simt.ui.features.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

internal fun shouldPlaceOnboardingTextAtTop(
    highlightedBounds: Rect,
    overlayHeightPx: Float,
): Boolean = overlayHeightPx > 0f && highlightedBounds.center.y >= overlayHeightPx / 2f

/**
 * A generic onboarding overlay that highlights a specific area of the screen
 * and provides a message to the user.
 *
 * ### Performance notes
 * - [pulseScaleState] and [arrowOffsetState] are kept as raw [androidx.compose.runtime.State] objects so their
 *   values are only read inside `graphicsLayer` / `offset` lambdas (layout or draw phase),
 *   avoiding per-frame recomposition of the entire tree.
 * - [androidx.compose.ui.graphics.Path] objects and [androidx.compose.ui.graphics.Brush] instances used in Canvas are cached with `remember`.
 * - Density-dependent pixel values (`dp.toPx()`) are pre-computed once.
 *
 * @param enabled When false the overlay renders nothing. The application owns the preference
 *   that hides tips (e.g. a "Show tips" settings toggle) and passes it in here.
 * @param highlightedBounds The bounds of the area to highlight in window coordinates.
 * @param onDismiss Callback when the overlay is dismissed
 * @param title The title text to display (optional)
 * @param description The description text to display
 * @param showArrow Whether to show an animated arrow pointing to the highlighted area
 * @param arrowFromBottom When true the arrow is placed **below** the highlighted area and points
 *   upward. Use this when the target component sits near the top of the screen. Defaults to false
 *   (arrow above, pointing down).
 * @param isCircle Whether to use a circular cutout (true) or rounded rect (false)
 * @param modifier Modifier for the overlay
 */
@Composable
public fun OnboardingOverlay(
    enabled: Boolean = true,
    highlightedBounds: Rect,
    onDismiss: () -> Unit,
    title: String? = null,
    description: String,
    showArrow: Boolean = true,
    arrowFromBottom: Boolean = false,
    isCircle: Boolean = true,
    modifier: Modifier = Modifier,
) {
    if (!enabled) return

    val infiniteTransition = rememberInfiniteTransition(label = "onboarding_pulse")

    // Keep as State — read only inside graphicsLayer / Canvas (no recomposition)
    val pulseScaleState: State<Float> =
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "pulse_scale",
        )

    val arrowOffsetState: State<Float> =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 20f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(800, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "arrow_offset",
        )

    val primaryColor = MaterialTheme.colorScheme.primary

    // Pre-compute static colours & density conversions
    val scrimColor = remember { Color.Black.copy(alpha = 0.75f) }
    val density = LocalDensity.current
    val strokeWidthPx = remember(density) { with(density) { 4.dp.toPx() } }
    val paddingPx = remember(density) { with(density) { 8.dp.toPx() } }
    val cornerRadiusPx = remember(density) { with(density) { 16.dp.toPx() } }

    // Cache Path objects for cutout and arrow
    val cutoutPath = remember { Path() }
    val arrowPath = remember { Path() }

    // Cache arrow gradient brush
    val arrowBrush =
        remember(primaryColor) {
            Brush.Companion.verticalGradient(
                colors = listOf(primaryColor.copy(alpha = 0.8f), primaryColor),
            )
        }
    val arrowStrokeColor = remember(primaryColor) { primaryColor.copy(alpha = 0.3f) }
    val layoutDirection = LocalLayoutDirection.current
    var overlayBoundsInWindow by remember { mutableStateOf(Rect.Zero) }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    overlayBoundsInWindow = coordinates.boundsInWindow()
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
    ) {
        val overlayWidthPx = with(density) { maxWidth.toPx() }
        val overlayHeightPx = with(density) { maxHeight.toPx() }
        val localHighlightedBounds =
            if (overlayBoundsInWindow == Rect.Zero) {
                highlightedBounds
            } else {
                Rect(
                    left = highlightedBounds.left - overlayBoundsInWindow.left,
                    top = highlightedBounds.top - overlayBoundsInWindow.top,
                    right = highlightedBounds.right - overlayBoundsInWindow.left,
                    bottom = highlightedBounds.bottom - overlayBoundsInWindow.top,
                )
            }
        val placeTextAtTop = shouldPlaceOnboardingTextAtTop(localHighlightedBounds, overlayHeightPx)
        // Scrim with cutout — all animated reads happen in draw-phase only
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cutoutRect = localHighlightedBounds
            val circleRadius = (maxOf(cutoutRect.width, cutoutRect.height) / 2f) * 1.4f
            val circleCenter = Offset(cutoutRect.center.x, cutoutRect.center.y)
            val pulseScale = pulseScaleState.value

            drawIntoCanvas { canvas ->
                val checkpoint = canvas.nativeCanvas.saveLayer(null, null)

                // Draw full scrim
                drawRect(color = scrimColor)

                if (isCircle) {
                    // Punch a circular hole
                    drawCircle(
                        color = Color.Transparent,
                        radius = circleRadius,
                        center = circleCenter,
                        blendMode = BlendMode.Companion.Clear,
                    )

                    // Glow ring
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.4f * (2f - pulseScale)),
                        radius = circleRadius * pulseScale,
                        center = circleCenter,
                        style = Stroke(width = strokeWidthPx),
                    )
                } else {
                    // Punch a rounded rect hole (reuse cached Path)
                    cutoutPath.reset()
                    cutoutPath.addRoundRect(
                        RoundRect(
                            left = cutoutRect.left - paddingPx,
                            top = cutoutRect.top - paddingPx,
                            right = cutoutRect.right + paddingPx,
                            bottom = cutoutRect.bottom + paddingPx,
                            cornerRadius = CornerRadius(cornerRadiusPx),
                        ),
                    )
                    drawPath(
                        path = cutoutPath,
                        color = Color.Transparent,
                        blendMode = BlendMode.Companion.Clear,
                    )

                    // Glow ring
                    val glowPadding = paddingPx * pulseScale
                    val glowRect =
                        RoundRect(
                            left = cutoutRect.left - glowPadding,
                            top = cutoutRect.top - glowPadding,
                            right = cutoutRect.right + glowPadding,
                            bottom = cutoutRect.bottom + glowPadding,
                            cornerRadius = CornerRadius(cornerRadiusPx * pulseScale),
                        )
                    drawRoundRect(
                        brush = SolidColor(primaryColor.copy(alpha = 0.4f * (2f - pulseScale))),
                        topLeft = Offset(glowRect.left, glowRect.top),
                        size = Size(glowRect.width, glowRect.height),
                        cornerRadius = CornerRadius(glowRect.bottomLeftCornerRadius.x),
                        style = Stroke(width = strokeWidthPx),
                    )
                }

                canvas.nativeCanvas.restoreToCount(checkpoint)
            }
        }

        if (showArrow) {
            // Arrow — animated offset applied via lambda-offset (layout-phase only, no recomposition)
            val arrowSizeDp = 60.dp
            val arrowSizePx = remember(density) { with(density) { 60.dp.toPx() } }
            val spacingPx = remember(density) { with(density) { 20.dp.toPx() } }

            val baseArrowYPx =
                if (arrowFromBottom) {
                    localHighlightedBounds.bottom + spacingPx
                } else {
                    localHighlightedBounds.top - arrowSizePx - spacingPx
                }

            // Center arrow on highlighted area. In RTL, offset x is from end so mirror.
            val arrowCenterXLtr = localHighlightedBounds.center.x - (arrowSizePx / 2f)
            val offsetX =
                when (layoutDirection) {
                    LayoutDirection.Ltr -> arrowCenterXLtr
                    LayoutDirection.Rtl -> overlayWidthPx - arrowSizePx - arrowCenterXLtr
                }

            Canvas(
                modifier =
                    Modifier
                        .size(arrowSizeDp)
                        .offset {
                            // Read arrowOffsetState here — only invalidates placement, not composition
                            val animOffsetPx = with(density) { arrowOffsetState.value.dp.toPx() }
                            IntOffset(
                                x = offsetX.roundToInt(),
                                // arrowFromBottom: drift downward (away from component); otherwise drift upward.
                                y =
                                    if (arrowFromBottom) {
                                        (baseArrowYPx + animOffsetPx).roundToInt()
                                    } else {
                                        (baseArrowYPx - animOffsetPx).roundToInt()
                                    },
                            )
                        },
            ) {
                val w = size.width
                val h = size.height

                // Reuse cached Path — shape depends on which side of the component the arrow is on
                arrowPath.reset()
                if (arrowFromBottom) {
                    arrowPath.moveTo(w / 2f, 0f) // Tip pointing up
                    arrowPath.lineTo(w * 0.2f, h * 0.6f)
                    arrowPath.lineTo(w * 0.4f, h * 0.6f)
                    arrowPath.lineTo(w * 0.4f, h)
                    arrowPath.lineTo(w * 0.6f, h)
                    arrowPath.lineTo(w * 0.6f, h * 0.6f)
                    arrowPath.lineTo(w * 0.8f, h * 0.6f)
                } else {
                    arrowPath.moveTo(w / 2f, h) // Tip pointing down
                    arrowPath.lineTo(w * 0.2f, h * 0.4f)
                    arrowPath.lineTo(w * 0.4f, h * 0.4f)
                    arrowPath.lineTo(w * 0.4f, 0f)
                    arrowPath.lineTo(w * 0.6f, 0f)
                    arrowPath.lineTo(w * 0.6f, h * 0.4f)
                    arrowPath.lineTo(w * 0.8f, h * 0.4f)
                }
                arrowPath.close()

                drawPath(path = arrowPath, brush = arrowBrush)
                drawPath(path = arrowPath, color = arrowStrokeColor, style = Stroke(width = strokeWidthPx / 2f))
            }
        }

        // Text Overlay — pulse applied via graphicsLayer (draw-phase only, no recomposition)
        Column(
            modifier =
                Modifier
                    .align(if (placeTextAtTop) Alignment.TopCenter else Alignment.BottomCenter)
                    .padding(horizontal = 40.dp)
                    .offset(y = if (placeTextAtTop) 96.dp else (-200).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (title != null) {
                Text(
                    text = title.uppercase(),
                    color = primaryColor,
                    style =
                        MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Companion.Bold,
                            letterSpacing = 2.sp,
                        ),
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            Text(
                text = description,
                color = Color.White,
                style =
                    MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Companion.ExtraBold,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Companion.Center,
                        lineHeight = 32.sp,
                    ),
                // graphicsLayer reads the animated value in draw-phase only — no recomposition
                modifier =
                    Modifier.Companion.graphicsLayer {
                        alpha = pulseScaleState.value
                    },
            )
        }
    }
}
