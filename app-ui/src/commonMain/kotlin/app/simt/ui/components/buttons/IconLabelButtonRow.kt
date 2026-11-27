package app.simt.ui.components.buttons

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.simt.ui.effects.ScrollEdgeEffect

public data class IconLabelAction(
    val key: String,
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val color: Color? = null,
    val backgroundColor: Color? = null,
    val isSelected: Boolean = false,
    val isPremium: Boolean = false,
    val tooltipText: String? = null,
    val onPositioned: ((LayoutCoordinates) -> Unit)? = null,
)

/** Uses a centered row while actions fit and a horizontal lazy list when they overflow. */
@Composable
public fun IconLabelButtonRow(
    actions: List<IconLabelAction>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 0.dp),
    itemSpacing: Dp = 12.dp,
    defaultColor: Color = MaterialTheme.colorScheme.primary,
    showOverflowIndicators: Boolean = true,
    isRedMode: Boolean = false,
    compact: Boolean = false,
) {
    val layoutDirection = LocalLayoutDirection.current

    SubcomposeLayout(modifier = modifier.fillMaxWidth()) { constraints ->
        val itemConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables =
            subcompose("measure") {
                actions.forEach { action ->
                    ActionButton(action, defaultColor, isRedMode, compact, forMeasurement = true)
                }
            }.map { it.measure(itemConstraints) }

        val totalSpacing = if (actions.size > 1) (actions.size - 1) * itemSpacing.roundToPx() else 0
        val contentPaddingPx =
            (
                contentPadding.calculateLeftPadding(layoutDirection) +
                    contentPadding.calculateRightPadding(layoutDirection)
            ).roundToPx()
        val totalContentWidth = placeables.sumOf { it.width } + totalSpacing + contentPaddingPx
        val fits = totalContentWidth <= constraints.maxWidth

        val mainPlaceable =
            subcompose("content") {
                if (fits) {
                    Row(
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        actions.forEach { action ->
                            ActionButton(action, defaultColor, isRedMode, compact)
                        }
                    }
                } else {
                    val scrollActions =
                        actions.map { action ->
                            if (action.isSelected) action else action.copy(backgroundColor = Color.Transparent)
                        }
                    ScrollableIconLabelButtonRow(
                        actions = scrollActions,
                        itemSpacing = itemSpacing,
                        contentPadding = contentPadding,
                        defaultColor = defaultColor,
                        showOverflowIndicators = showOverflowIndicators,
                        isRedMode = isRedMode,
                        compact = compact,
                    )
                }
            }.first().measure(constraints)

        layout(mainPlaceable.width, mainPlaceable.height) {
            mainPlaceable.place(0, 0)
        }
    }
}

@Composable
private fun ScrollableIconLabelButtonRow(
    actions: List<IconLabelAction>,
    itemSpacing: Dp,
    contentPadding: PaddingValues,
    defaultColor: Color,
    showOverflowIndicators: Boolean,
    isRedMode: Boolean,
    compact: Boolean,
) {
    val listState = rememberLazyListState()

    ScrollEdgeEffect(
        state = listState,
        orientation = Orientation.Horizontal,
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        showArrows = showOverflowIndicators,
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            contentPadding = contentPadding,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(items = actions, key = { it.key }) { action ->
                ActionButton(action, defaultColor, isRedMode, compact)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionButton(
    action: IconLabelAction,
    defaultColor: Color,
    isRedMode: Boolean,
    compact: Boolean = false,
    forMeasurement: Boolean = false,
) {
    val positionModifier =
        if (action.onPositioned != null && !forMeasurement) {
            Modifier.onGloballyPositioned(action.onPositioned)
        } else {
            Modifier
        }
    val measuredModifier = if (forMeasurement) positionModifier.clearAndSetSemantics {} else positionModifier
    val actionModifier =
        if (forMeasurement) {
            measuredModifier
        } else {
            measuredModifier.semantics {
                if (action.isSelected) selected = true
            }
        }
    val button: @Composable () -> Unit = {
        if (action.isPremium && !isRedMode) {
            PremiumIconLabelButton(
                onClick = action.onClick,
                icon = action.icon,
                label = action.label,
                enabled = action.enabled,
                modifier = actionModifier,
                compact = compact,
            )
        } else {
            IconLabelButton(
                onClick = action.onClick,
                icon = action.icon,
                label = action.label,
                color = action.color ?: defaultColor,
                backgroundColor = action.backgroundColor ?: Color.Transparent,
                enabled = action.enabled,
                modifier = actionModifier,
                compact = compact,
            )
        }
    }

    // Null uses the label; an explicitly blank value suppresses the tooltip.
    val effectiveTooltip = action.tooltipText ?: action.label
    if (!forMeasurement && effectiveTooltip.isNotBlank()) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text(effectiveTooltip) } },
            state = rememberTooltipState(),
        ) {
            button()
        }
    } else {
        button()
    }
}
