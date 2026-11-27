package app.simt.ui.components.forms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/** Single-choice options/styling mirror [AppMultiChoice]'s — same shape, at-most-one selection. */
public typealias AppSingleChoiceOption<T> = AppMultiChoiceOption<T>
public typealias AppSingleChoiceLayout = AppMultiChoiceLayout
public typealias AppSingleChoiceColor = AppMultiChoiceColor
public typealias AppSingleChoiceIconDisplay = AppMultiChoiceIconDisplay

/**
 * The working selection owned by an [AppSingleChoice]. Callers seed it from their persisted value
 * via [rememberAppSingleChoiceState] and read [selected] when they need the result; the component
 * performs selection and honours [required].
 *
 * @param required when `true` (default) at least one option stays selected — tapping the selected
 *   option is a no-op. When `false`, zero selection is allowed and tapping the selected option
 *   clears it (`selected` becomes `null`).
 */
@Stable
public class AppSingleChoiceState<T>(
    initialSelected: T?,
    public val required: Boolean = true,
) {
    public var selected: T? by mutableStateOf(initialSelected)
        private set

    /** Selects [value]; when not [required], selecting the already-selected value clears it. */
    public fun select(value: T) {
        selected = if (selected == value && !required) null else value
    }
}

/**
 * Remembers an [AppSingleChoiceState] seeded with [initialSelected]. Re-created when the
 * [initialSelected] identity or [required] changes, so re-opening a screen with a different
 * persisted value resets the working selection.
 */
@Composable
public fun <T> rememberAppSingleChoiceState(
    initialSelected: T?,
    required: Boolean = true,
): AppSingleChoiceState<T> = remember(initialSelected, required) { AppSingleChoiceState(initialSelected, required) }

/**
 * A generic single-select chip group built on Material 3 `FilterChip` — the at-most-one sibling of
 * [AppMultiChoice], sharing its layout, colors, icon, and tooltip behaviour. Used wherever the user
 * picks exactly one (or, with [AppSingleChoiceState.required] `= false`, at most one) item from a
 * small set.
 *
 * @param options The choices to render, in order. A `null`-valued option is supported (e.g. an
 *   "Any" chip) and reads as selected when nothing else is.
 * @param state Working selection holder, typically from [rememberAppSingleChoiceState].
 * @param modifier Modifier for the container.
 * @param layout [AppSingleChoiceLayout.Wrap] (FlowRow) or [AppSingleChoiceLayout.Scroll] (scrollable Row).
 * @param selectedColor Theme color for the selected chip container/label/icon.
 * @param iconDisplay Whether option icons show [AppSingleChoiceIconDisplay.Always] or only when selected.
 * @param onSelectionChange Optional callback invoked with the new selection after every change, for
 *   sites that persist immediately rather than on a confirm action.
 */
@Composable
public fun <T> AppSingleChoice(
    options: List<AppSingleChoiceOption<T>>,
    state: AppSingleChoiceState<T>,
    modifier: Modifier = Modifier,
    layout: AppSingleChoiceLayout = AppSingleChoiceLayout.Wrap,
    selectedColor: AppSingleChoiceColor = AppSingleChoiceColor.Secondary,
    iconDisplay: AppSingleChoiceIconDisplay = AppSingleChoiceIconDisplay.Always,
    onSelectionChange: ((T?) -> Unit)? = null,
) {
    ChoiceChipGroup(
        options = options,
        isSelected = { it == state.selected },
        onClick = { value ->
            state.select(value)
            onSelectionChange?.invoke(state.selected)
        },
        modifier = modifier,
        layout = layout,
        selectedColor = selectedColor,
        iconDisplay = iconDisplay,
    )
}
