package app.shared.ui.effects

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

// Content has no BoxScope receiver to preserve callers' surrounding layout scopes.
@Composable
public fun ScrollEdgeEffect(
    state: ScrollableState,
    orientation: Orientation,
    modifier: Modifier = Modifier,
    fade: Boolean = true,
    fadeSize: Dp = ScrollEdgeEffectDefaults.fadeSize(orientation),
    showArrows: Boolean = true,
    arrowTint: Color = ScrollEdgeEffectDefaults.arrowTint(),
    reverseDirection: Boolean = false,
    content: @Composable () -> Unit,
) {
    val canScrollTowardsStart by
        remember(state, reverseDirection) {
            derivedStateOf { if (reverseDirection) state.canScrollForward else state.canScrollBackward }
        }
    val canScrollTowardsEnd by
        remember(state, reverseDirection) {
            derivedStateOf { if (reverseDirection) state.canScrollBackward else state.canScrollForward }
        }

    val motionEnabled = LocalUiMotionEnabled.current
    val startFade =
        if (motionEnabled) {
            val progress by animateFloatAsState(targetValue = if (canScrollTowardsStart) 1f else 0f, label = "scrollEdgeFadeStart")
            progress
        } else {
            if (canScrollTowardsStart) 1f else 0f
        }
    val endFade =
        if (motionEnabled) {
            val progress by animateFloatAsState(targetValue = if (canScrollTowardsEnd) 1f else 0f, label = "scrollEdgeFadeEnd")
            progress
        } else {
            if (canScrollTowardsEnd) 1f else 0f
        }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Box(modifier = modifier) {
        Box(
            modifier =
                if (fade && fadeSize > 0.dp && (startFade > 0f || endFade > 0f)) {
                    Modifier
                        // DstIn must composite against transparent content, not the parent's pixels.
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            val edgePx = fadeSize.toPx()
                            when (orientation) {
                                Orientation.Vertical -> {
                                    if (startFade > 0f) drawEdgeFade(ContentEdge.Top, startFade, edgePx)
                                    if (endFade > 0f) drawEdgeFade(ContentEdge.Bottom, endFade, edgePx)
                                }
                                Orientation.Horizontal -> {
                                    if (startFade > 0f) drawEdgeFade(if (isRtl) ContentEdge.Right else ContentEdge.Left, startFade, edgePx)
                                    if (endFade > 0f) drawEdgeFade(if (isRtl) ContentEdge.Left else ContentEdge.Right, endFade, edgePx)
                                }
                            }
                        }
                } else {
                    Modifier
                },
        ) {
            content()
        }

        if (showArrows) {
            // Match resolved content bounds without affecting the viewport's measurement.
            Box(modifier = Modifier.matchParentSize()) {
                ScrollEdgeArrowOverlay(
                    visible = canScrollTowardsStart,
                    orientation = orientation,
                    edge = ScrollEdgeSide.Start,
                    fadeSize = fadeSize,
                    arrowTint = arrowTint,
                )
                ScrollEdgeArrowOverlay(
                    visible = canScrollTowardsEnd,
                    orientation = orientation,
                    edge = ScrollEdgeSide.End,
                    fadeSize = fadeSize,
                    arrowTint = arrowTint,
                )
            }
        }
    }
}

public object ScrollEdgeEffectDefaults {
    public val HorizontalFadeSize: Dp = 48.dp
    public val VerticalFadeSize: Dp = 28.dp
    public val ArrowShift: Dp = 6.dp
    public val ArrowAnimationDurationMs: Int = 1200

    public fun fadeSize(orientation: Orientation): Dp = if (orientation == Orientation.Horizontal) HorizontalFadeSize else VerticalFadeSize

    @Composable
    public fun arrowTint(): Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
}

private enum class ScrollEdgeSide { Start, End }

private enum class ContentEdge { Top, Bottom, Left, Right }

/** Multiplicative alpha mask: progress zero keeps content; one removes the outermost pixels. */
private fun DrawScope.drawEdgeFade(
    edge: ContentEdge,
    progress: Float,
    edgePx: Float,
) {
    val outerAlpha = 1f - progress
    when (edge) {
        ContentEdge.Top ->
            drawRect(
                brush = Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = outerAlpha), Color.Black), startY = 0f, endY = edgePx),
                topLeft = Offset.Zero,
                size = Size(size.width, edgePx),
                blendMode = BlendMode.DstIn,
            )
        ContentEdge.Bottom ->
            drawRect(
                brush = Brush.verticalGradient(colors = listOf(Color.Black, Color.Black.copy(alpha = outerAlpha)), startY = size.height - edgePx, endY = size.height),
                topLeft = Offset(0f, size.height - edgePx),
                size = Size(size.width, edgePx),
                blendMode = BlendMode.DstIn,
            )
        ContentEdge.Left ->
            drawRect(
                brush = Brush.horizontalGradient(colors = listOf(Color.Black.copy(alpha = outerAlpha), Color.Black), startX = 0f, endX = edgePx),
                topLeft = Offset.Zero,
                size = Size(edgePx, size.height),
                blendMode = BlendMode.DstIn,
            )
        ContentEdge.Right ->
            drawRect(
                brush = Brush.horizontalGradient(colors = listOf(Color.Black, Color.Black.copy(alpha = outerAlpha)), startX = size.width - edgePx, endX = size.width),
                topLeft = Offset(size.width - edgePx, 0f),
                size = Size(edgePx, size.height),
                blendMode = BlendMode.DstIn,
            )
    }
}

@Composable
private fun BoxScope.ScrollEdgeArrowOverlay(
    visible: Boolean,
    orientation: Orientation,
    edge: ScrollEdgeSide,
    fadeSize: Dp,
    arrowTint: Color,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val motionEnabled = LocalUiMotionEnabled.current
    val alignment =
        when {
            orientation == Orientation.Vertical && edge == ScrollEdgeSide.Start -> Alignment.TopCenter
            orientation == Orientation.Vertical -> Alignment.BottomCenter
            edge == ScrollEdgeSide.Start -> Alignment.CenterStart
            else -> Alignment.CenterEnd
        }
    val bandModifier =
        if (orientation == Orientation.Vertical) {
            Modifier.fillMaxWidth().height(fadeSize)
        } else {
            Modifier.fillMaxHeight().width(fadeSize)
        }

    val overlayModifier = Modifier.align(alignment).then(bandModifier)
    if (motionEnabled) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = overlayModifier,
        ) {
            ScrollEdgeArrowBand(orientation, edge, arrowTint, isRtl)
        }
    } else if (visible) {
        // Static visibility is zero-duration, with no transition frames or animation allocation.
        Box(modifier = overlayModifier) {
            ScrollEdgeArrowBand(orientation, edge, arrowTint, isRtl)
        }
    }
}

@Composable
private fun ScrollEdgeArrowBand(
    orientation: Orientation,
    edge: ScrollEdgeSide,
    arrowTint: Color,
    isRtl: Boolean,
) {
    Box(
        // No pointer handlers; cleared semantics and null icon descriptions are decorative only.
        modifier = Modifier.fillMaxSize().clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        ScrollEdgeArrow(
            orientation = orientation,
            edge = edge,
            tint = arrowTint,
            isRtl = isRtl,
        )
    }
}

@Composable
private fun ScrollEdgeArrow(
    orientation: Orientation,
    edge: ScrollEdgeSide,
    tint: Color,
    isRtl: Boolean,
) {
    val density = LocalDensity.current
    val maxShiftPx = with(density) { ScrollEdgeEffectDefaults.ArrowShift.toPx() }
    val shiftX: Float
    val shiftY: Float
    when {
        orientation == Orientation.Vertical && edge == ScrollEdgeSide.Start -> {
            shiftX = 0f
            shiftY = -maxShiftPx
        }
        orientation == Orientation.Vertical -> {
            shiftX = 0f
            shiftY = maxShiftPx
        }
        edge == ScrollEdgeSide.Start -> {
            shiftX = if (isRtl) maxShiftPx else -maxShiftPx
            shiftY = 0f
        }
        else -> {
            shiftX = if (isRtl) -maxShiftPx else maxShiftPx
            shiftY = 0f
        }
    }

    val progress =
        if (LocalUiMotionEnabled.current) {
            val transition = rememberInfiniteTransition(label = "scroll_edge_arrow")
            val animatedProgress by
                transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(ScrollEdgeEffectDefaults.ArrowAnimationDurationMs, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse,
                        ),
                    label = "scroll_edge_arrow_shift",
                )
            animatedProgress
        } else {
            0f
        }
    val icon: ImageVector =
        when {
            orientation == Orientation.Vertical && edge == ScrollEdgeSide.Start -> Icons.Filled.KeyboardArrowUp
            orientation == Orientation.Vertical -> Icons.Filled.KeyboardArrowDown
            edge == ScrollEdgeSide.Start -> Icons.AutoMirrored.Filled.KeyboardArrowLeft
            else -> Icons.AutoMirrored.Filled.KeyboardArrowRight
        }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier =
            Modifier.graphicsLayer {
                translationX = shiftX * progress
                translationY = shiftY * progress
            },
    )
}
