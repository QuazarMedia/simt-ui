package app.simt.ui.components.buttons

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.simt.ui.effects.LocalUiMotionEnabled

@Immutable
public data class RoundMultiButtonItem(
    val icon: ImageVector,
    val onClick: () -> Unit,
    val contentDescription: String? = null,
)

public enum class RoundMultiButtonExpandAxis {
    Vertical,
    Horizontal,
}

public enum class RoundMultiButtonExpandDirection {
    Up,
    Down,
    Start,
    End,
}

/** Expands caller-provided icon actions around a main button with transient local disclosure state. */
@Composable
public fun RoundMultiButton(
    icon: ImageVector,
    items: List<RoundMultiButtonItem>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    tint: Color = Color.White,
    backgroundColor: Color = Color(0x33000000),
    borderColor: Color = Color(0x40FFFFFF),
    expandAxis: RoundMultiButtonExpandAxis = RoundMultiButtonExpandAxis.Vertical,
    expandDirection: RoundMultiButtonExpandDirection = RoundMultiButtonExpandDirection.Up,
) {
    var expanded by remember { mutableStateOf(false) }
    val motionEnabled = LocalUiMotionEnabled.current
    val isExpanded = expanded && enabled && items.isNotEmpty()

    LaunchedEffect(enabled, items.isEmpty()) {
        if (!enabled || items.isEmpty()) expanded = false
    }

    val safeDirection =
        when (expandAxis) {
            RoundMultiButtonExpandAxis.Vertical ->
                when (expandDirection) {
                    RoundMultiButtonExpandDirection.Up,
                    RoundMultiButtonExpandDirection.Down,
                    -> expandDirection
                    RoundMultiButtonExpandDirection.Start,
                    RoundMultiButtonExpandDirection.End,
                    -> RoundMultiButtonExpandDirection.Up
                }
            RoundMultiButtonExpandAxis.Horizontal ->
                when (expandDirection) {
                    RoundMultiButtonExpandDirection.Start,
                    RoundMultiButtonExpandDirection.End,
                    -> expandDirection
                    RoundMultiButtonExpandDirection.Up,
                    RoundMultiButtonExpandDirection.Down,
                    -> RoundMultiButtonExpandDirection.End
                }
        }

    val onItemClick: (RoundMultiButtonItem) -> Unit = { item ->
        expanded = false
        item.onClick()
    }
    val mainButton: @Composable () -> Unit = {
        RoundMultiButtonMainButton(
            icon = icon,
            onClick = { expanded = if (items.isEmpty()) false else !expanded },
            enabled = enabled,
            expanded = isExpanded,
            motionEnabled = motionEnabled,
            contentDescription = contentDescription,
            tint = tint,
            backgroundColor = backgroundColor,
            borderColor = borderColor,
        )
    }
    val expandedContent: @Composable () -> Unit = {
        RoundMultiButtonExpandedContent(
            items = items,
            visible = isExpanded,
            motionEnabled = motionEnabled,
            axis = expandAxis,
            direction = safeDirection,
            enabled = enabled,
            tint = tint,
            backgroundColor = backgroundColor,
            borderColor = borderColor,
            onItemClick = onItemClick,
        )
    }

    when (expandAxis) {
        RoundMultiButtonExpandAxis.Vertical ->
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                if (safeDirection == RoundMultiButtonExpandDirection.Up) {
                    expandedContent()
                    if (isExpanded) ConnectorSpacer(RoundMultiButtonExpandAxis.Vertical, borderColor)
                    mainButton()
                } else {
                    mainButton()
                    if (isExpanded) ConnectorSpacer(RoundMultiButtonExpandAxis.Vertical, borderColor)
                    expandedContent()
                }
            }
        RoundMultiButtonExpandAxis.Horizontal ->
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                if (safeDirection == RoundMultiButtonExpandDirection.Start) {
                    expandedContent()
                    if (isExpanded) ConnectorSpacer(RoundMultiButtonExpandAxis.Horizontal, borderColor)
                    mainButton()
                } else {
                    mainButton()
                    if (isExpanded) ConnectorSpacer(RoundMultiButtonExpandAxis.Horizontal, borderColor)
                    expandedContent()
                }
            }
    }
}

@Composable
private fun RoundMultiButtonMainButton(
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    expanded: Boolean,
    motionEnabled: Boolean,
    contentDescription: String?,
    tint: Color,
    backgroundColor: Color,
    borderColor: Color,
) {
    val targetScale = if (enabled && expanded) 1.1f else 1f
    val scale =
        if (motionEnabled) {
            val animatedScale by animateFloatAsState(targetScale, tween(200), label = "roundMultiButtonMainScale")
            animatedScale
        } else {
            targetScale
        }
    val targetBackgroundColor =
        when {
            !enabled -> MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
            expanded -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
            else -> backgroundColor
        }
    val animatedBackgroundColor =
        if (motionEnabled) {
            val color by animateColorAsState(targetBackgroundColor, tween(300), label = "roundMultiButtonMainBackground")
            color
        } else {
            targetBackgroundColor
        }
    val targetIconColor =
        when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            expanded -> MaterialTheme.colorScheme.primary
            else -> tint
        }
    val animatedIconColor =
        if (motionEnabled) {
            val color by animateColorAsState(targetIconColor, tween(300), label = "roundMultiButtonMainIconColor")
            color
        } else {
            targetIconColor
        }
    val borderWidth: Dp = if (expanded && enabled) 2.dp else 1.dp
    val targetBorderColor =
        when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            expanded -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else -> borderColor
        }
    val animatedBorderColor =
        if (motionEnabled) {
            val color by animateColorAsState(targetBorderColor, tween(300), label = "roundMultiButtonMainBorderColor")
            color
        } else {
            targetBorderColor
        }
    val ringColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    val ringModifier =
        if (expanded && enabled) {
            Modifier.drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(listOf(ringColor, Color.Transparent)),
                    radius = size.minDimension / 2f,
                )
            }
        } else {
            Modifier
        }

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier =
            ringModifier
                .scale(scale)
                .background(animatedBackgroundColor, shape = CircleShape)
                .border(borderWidth, animatedBorderColor, CircleShape),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = animatedIconColor,
        )
    }
}

@Composable
private fun RoundMultiButtonExpandedContent(
    items: List<RoundMultiButtonItem>,
    visible: Boolean,
    motionEnabled: Boolean,
    axis: RoundMultiButtonExpandAxis,
    direction: RoundMultiButtonExpandDirection,
    enabled: Boolean,
    tint: Color,
    backgroundColor: Color,
    borderColor: Color,
    onItemClick: (RoundMultiButtonItem) -> Unit,
) {
    val content: @Composable () -> Unit = {
        RoundMultiButtonItems(
            items = items,
            axis = axis,
            enabled = enabled,
            tint = tint,
            backgroundColor = backgroundColor,
            borderColor = borderColor,
            onItemClick = onItemClick,
        )
    }
    if (motionEnabled) {
        val enter =
            when (axis) {
                RoundMultiButtonExpandAxis.Vertical ->
                    fadeIn(tween(120)) +
                        expandVertically(
                            animationSpec = tween(140),
                            expandFrom = if (direction == RoundMultiButtonExpandDirection.Up) Alignment.Bottom else Alignment.Top,
                        )
                RoundMultiButtonExpandAxis.Horizontal ->
                    fadeIn(tween(120)) +
                        expandHorizontally(
                            animationSpec = tween(140),
                            expandFrom = if (direction == RoundMultiButtonExpandDirection.Start) Alignment.End else Alignment.Start,
                        )
            }
        val exit =
            when (axis) {
                RoundMultiButtonExpandAxis.Vertical ->
                    fadeOut(tween(90)) +
                        shrinkVertically(
                            animationSpec = tween(110),
                            shrinkTowards = if (direction == RoundMultiButtonExpandDirection.Up) Alignment.Bottom else Alignment.Top,
                        )
                RoundMultiButtonExpandAxis.Horizontal ->
                    fadeOut(tween(90)) +
                        shrinkHorizontally(
                            animationSpec = tween(110),
                            shrinkTowards = if (direction == RoundMultiButtonExpandDirection.Start) Alignment.End else Alignment.Start,
                        )
            }
        AnimatedVisibility(
            visible = visible,
            enter = enter,
            exit = exit,
        ) {
            content()
        }
    } else if (visible) {
        content()
    }
}

@Composable
private fun RoundMultiButtonItems(
    items: List<RoundMultiButtonItem>,
    axis: RoundMultiButtonExpandAxis,
    enabled: Boolean,
    tint: Color,
    backgroundColor: Color,
    borderColor: Color,
    onItemClick: (RoundMultiButtonItem) -> Unit,
) {
    when (axis) {
        RoundMultiButtonExpandAxis.Vertical ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                items.forEachIndexed { index, item ->
                    OverlayIconButton(
                        icon = item.icon,
                        onClick = { onItemClick(item) },
                        enabled = enabled,
                        contentDescription = item.contentDescription,
                        tint = tint,
                        backgroundColor = backgroundColor,
                        borderColor = borderColor,
                    )
                    if (index != items.lastIndex) ConnectorSpacer(axis, borderColor)
                }
            }
        RoundMultiButtonExpandAxis.Horizontal ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                items.forEachIndexed { index, item ->
                    OverlayIconButton(
                        icon = item.icon,
                        onClick = { onItemClick(item) },
                        enabled = enabled,
                        contentDescription = item.contentDescription,
                        tint = tint,
                        backgroundColor = backgroundColor,
                        borderColor = borderColor,
                    )
                    if (index != items.lastIndex) ConnectorSpacer(axis, borderColor)
                }
            }
    }
}

@Composable
private fun ConnectorSpacer(
    axis: RoundMultiButtonExpandAxis,
    color: Color,
) {
    val modifier =
        when (axis) {
            RoundMultiButtonExpandAxis.Vertical -> Modifier.height(8.dp).width(2.dp)
            RoundMultiButtonExpandAxis.Horizontal -> Modifier.width(8.dp).height(2.dp)
        }
    Spacer(modifier = modifier.background(color, shape = CircleShape))
}
