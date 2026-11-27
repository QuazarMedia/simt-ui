package app.simt.ui.components.forms

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import app.simt.ui.effects.ScrollEdgeEffect

/** How the choices are arranged when they don't fit on a single line. */
public enum class AppMultiChoiceLayout {
    /** Chips wrap onto new lines via a [FlowRow]. */
    Wrap,

    /** Chips stay on one horizontally-scrollable [Row], decorated with a [ScrollEdgeEffect]. */
    Scroll,
}

/** Theme color used to tint a chip's selected state. */
public enum class AppMultiChoiceColor {
    Primary,
    Secondary,
    Tertiary,
}

/** When an option's [AppMultiChoiceOption.icon] should be shown. */
public enum class AppMultiChoiceIconDisplay {
    /** Always show the icon (only for options that actually declare one). */
    Always,

    /**
     * Show the icon only while the option is selected. A selected option without its own
     * [AppMultiChoiceOption.icon] falls back to [Icons.Default.CheckCircle].
     */
    WhenSelected,
}

/**
 * A single selectable option in an [AppMultiChoice].
 *
 * @param value Stable identity used for selection and toggling.
 * @param label Text shown on the chip.
 * @param tooltip Optional long-press plain tooltip — useful when [label] is terse/ambiguous.
 * @param icon Optional leading icon (subject to [AppMultiChoiceIconDisplay]).
 * @param iconColor Optional explicit icon tint; `null` inherits the chip's content color.
 * @param enabled Whether the chip is interactive.
 * @param highlighted Emphasizes the chip with a distinct (primary) container color to mark it out
 *   from the rest — e.g. the pinned "default" item. Independent of selection styling.
 */
@Immutable
public data class AppMultiChoiceOption<T>(
    val value: T,
    val label: String,
    val tooltip: String? = null,
    val icon: ImageVector? = null,
    val iconColor: Color? = null,
    val enabled: Boolean = true,
    val highlighted: Boolean = false,
)

/**
 * The working selection owned by an [AppMultiChoice]. Callers seed it from their persisted value
 * via [rememberAppMultiChoiceState] and read [selected] when they need the result (e.g. at dialog
 * confirm). The component performs all toggling and enforces [minSelected], so callers no longer
 * keep their own selection set or toggle logic.
 */
@Stable
public class AppMultiChoiceState<T>(
    initialSelected: Set<T>,
    public val minSelected: Int = 0,
) {
    public var selected: Set<T> by mutableStateOf(initialSelected)
        private set

    /** Adds [value] if absent, removes it if present — but never drops below [minSelected]. */
    public fun toggle(value: T) {
        selected =
            when {
                value !in selected -> selected + value
                selected.size > minSelected -> selected - value
                else -> selected
            }
    }

    /** Ensures [value] is part of the selection, adding it if absent. Never removes anything. */
    public fun select(value: T) {
        if (value !in selected) selected = selected + value
    }
}

/**
 * Remembers an [AppMultiChoiceState] seeded with [initialSelected]. The state is re-created when the
 * [initialSelected] identity or [minSelected] changes, so re-opening a dialog with a different
 * persisted value resets the working selection.
 */
@Composable
public fun <T> rememberAppMultiChoiceState(
    initialSelected: Set<T>,
    minSelected: Int = 0,
): AppMultiChoiceState<T> = remember(initialSelected, minSelected) { AppMultiChoiceState(initialSelected, minSelected) }

/**
 * A generic multi-select chip group built on Material 3 [FilterChip]. Used across the app wherever
 * the user picks several items from a small set (GNSS constellations, profile views, automation
 * days, map filters).
 *
 * The component owns the *working* selection ([state]) but not the *source of truth*: callers either
 * read [AppMultiChoiceState.selected] at confirm time, or persist eagerly through [onSelectionChange].
 *
 * @param options The choices to render, in order.
 * @param state Working selection holder, typically from [rememberAppMultiChoiceState].
 * @param modifier Modifier for the container.
 * @param layout [AppMultiChoiceLayout.Wrap] (FlowRow) or [AppMultiChoiceLayout.Scroll] (scrollable Row).
 * @param selectedColor Theme color for the selected chip container/label/icon.
 * @param iconDisplay Whether option icons show [AppMultiChoiceIconDisplay.Always] or only when selected.
 * @param onSelectionChange Optional callback invoked with the new selection after every toggle, for
 *   sites that persist immediately rather than on a confirm action.
 * @param onLongClick Optional callback invoked with an option's value when its chip is long-pressed.
 *   When set, a long press fires this instead of toggling selection (the tap still toggles).
 */
@Composable
public fun <T> AppMultiChoice(
    options: List<AppMultiChoiceOption<T>>,
    state: AppMultiChoiceState<T>,
    modifier: Modifier = Modifier,
    layout: AppMultiChoiceLayout = AppMultiChoiceLayout.Wrap,
    selectedColor: AppMultiChoiceColor = AppMultiChoiceColor.Secondary,
    iconDisplay: AppMultiChoiceIconDisplay = AppMultiChoiceIconDisplay.Always,
    onSelectionChange: ((Set<T>) -> Unit)? = null,
    onLongClick: ((T) -> Unit)? = null,
) {
    ChoiceChipGroup(
        options = options,
        isSelected = { it in state.selected },
        onClick = { value ->
            state.toggle(value)
            onSelectionChange?.invoke(state.selected)
        },
        modifier = modifier,
        layout = layout,
        selectedColor = selectedColor,
        iconDisplay = iconDisplay,
        onLongClick = onLongClick,
    )
}

/**
 * Shared chip-group renderer behind [AppMultiChoice] and `AppSingleChoice`. Lays the [options] out
 * (wrapping [FlowRow] or horizontally-scrollable [Row]) and renders each as a [FilterChip]; the
 * owning component decides selection semantics via [isSelected] and [onClick].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun <T> ChoiceChipGroup(
    options: List<AppMultiChoiceOption<T>>,
    isSelected: (T) -> Boolean,
    onClick: (T) -> Unit,
    modifier: Modifier = Modifier,
    layout: AppMultiChoiceLayout = AppMultiChoiceLayout.Wrap,
    selectedColor: AppMultiChoiceColor = AppMultiChoiceColor.Secondary,
    iconDisplay: AppMultiChoiceIconDisplay = AppMultiChoiceIconDisplay.Always,
    onLongClick: ((T) -> Unit)? = null,
) {
    val colors = selectedChipColors(selectedColor)
    val highlightColors = highlightedChipColors()

    when (layout) {
        AppMultiChoiceLayout.Wrap ->
            FlowRow(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                options.forEach { option ->
                    ChoiceChip(
                        option = option,
                        selected = isSelected(option.value),
                        colors = if (option.highlighted) highlightColors else colors,
                        iconDisplay = iconDisplay,
                        onClick = { onClick(option.value) },
                        onLongClick = onLongClick?.let { { it(option.value) } },
                    )
                }
            }

        AppMultiChoiceLayout.Scroll -> {
            val scrollState = rememberScrollState()
            ScrollEdgeEffect(
                state = scrollState,
                orientation = Orientation.Horizontal,
                modifier = modifier.fillMaxWidth(),
                fadeSize = 24.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    options.forEach { option ->
                        ChoiceChip(
                            option = option,
                            selected = isSelected(option.value),
                            colors = if (option.highlighted) highlightColors else colors,
                            iconDisplay = iconDisplay,
                            onClick = { onClick(option.value) },
                            onLongClick = onLongClick?.let { { it(option.value) } },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChoiceChip(
    option: AppMultiChoiceOption<T>,
    selected: Boolean,
    colors: SelectableChipColors,
    iconDisplay: AppMultiChoiceIconDisplay,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val iconToShow: ImageVector? =
        when (iconDisplay) {
            // Always: only options that declare an icon get one.
            AppMultiChoiceIconDisplay.Always -> option.icon
            // WhenSelected: the selected chip always carries an icon — its own, or a CheckCircle
            // fallback so the selection still reads as a checked item when none was provided.
            AppMultiChoiceIconDisplay.WhenSelected ->
                if (selected) option.icon ?: Icons.Default.CheckCircle else null
        }
    val leadingIcon: (@Composable () -> Unit)? =
        iconToShow?.let { icon ->
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = option.iconColor ?: LocalContentColor.current,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        }

    val chip: @Composable () -> Unit = {
        FilterChip(
            selected = selected,
            onClick = onClick,
            enabled = option.enabled,
            label = { Text(option.label) },
            leadingIcon = leadingIcon,
            colors = colors,
            modifier =
                if (onLongClick != null && option.enabled) {
                    Modifier.chipLongPress(key = option.value, onLongPress = onLongClick)
                } else {
                    Modifier
                },
        )
    }

    val tooltip = option.tooltip
    if (tooltip != null) {
        // Long-press surfaces the fuller [tooltip] text — useful when the label is terse or
        // ambiguous (e.g. single-letter day-of-week chips that share an initial).
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text(tooltip) } },
            state = rememberTooltipState(),
        ) {
            chip()
        }
    } else {
        chip()
    }
}

@Composable
private fun selectedChipColors(color: AppMultiChoiceColor): SelectableChipColors {
    val scheme = MaterialTheme.colorScheme
    val container: Color
    val content: Color
    when (color) {
        AppMultiChoiceColor.Primary -> {
            container = scheme.primaryContainer
            content = scheme.onPrimaryContainer
        }
        AppMultiChoiceColor.Secondary -> {
            container = scheme.secondaryContainer
            content = scheme.onSecondaryContainer
        }
        AppMultiChoiceColor.Tertiary -> {
            container = scheme.tertiaryContainer
            content = scheme.onTertiaryContainer
        }
    }
    return FilterChipDefaults.filterChipColors(
        selectedContainerColor = container,
        selectedLabelColor = content,
        selectedLeadingIconColor = content,
    )
}

/**
 * Colors for a [AppMultiChoiceOption.highlighted] chip — a primary-tinted container that stands
 * out from the (secondary/tertiary) selected chips so the emphasized item reads as special.
 */
@Composable
private fun highlightedChipColors(): SelectableChipColors {
    val scheme = MaterialTheme.colorScheme
    return FilterChipDefaults.filterChipColors(
        selectedContainerColor = scheme.primaryContainer,
        selectedLabelColor = scheme.onPrimaryContainer,
        selectedLeadingIconColor = scheme.onPrimaryContainer,
    )
}

/**
 * Adds long-press detection to an already-clickable chip without stealing its tap.
 *
 * [FilterChip] only exposes `onClick`, so we watch the gesture on the [Initial][PointerEventPass]
 * pass (which runs before the chip's own click node): a normal tap is left untouched and handled by
 * the chip, while a hold past the long-press timeout fires [onLongPress] and consumes the remainder
 * of the gesture so the chip's `onClick` does not also fire on release.
 */
private fun Modifier.chipLongPress(
    key: Any?,
    onLongPress: () -> Unit,
): Modifier =
    pointerInput(key) {
        val longPressTimeoutMillis = viewConfiguration.longPressTimeoutMillis
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            try {
                withTimeout(longPressTimeoutMillis) {
                    waitForUpOrCancellation(PointerEventPass.Initial)
                }
                // Released (or cancelled) before the threshold → treat as a tap and let the chip
                // handle it: leave the gesture unconsumed.
            } catch (_: PointerEventTimeoutCancellationException) {
                onLongPress()
                waitForUpOrCancellation(PointerEventPass.Initial)?.consume()
            }
        }
    }
