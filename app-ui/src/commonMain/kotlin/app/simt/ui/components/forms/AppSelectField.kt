package app.simt.ui.components.forms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.simt.ui.effects.ScrollEdgeEffect
import app.simt.ui.resources.Res
import app.simt.ui.resources.simt_ui_cd_selected
import org.jetbrains.compose.resources.stringResource

/**
 * Default cap for an [AppSelectField] dropdown's height. Once the options exceed this (or the
 * available on-screen height, whichever is smaller) the menu scrolls instead of growing taller.
 */
private val AppSelectFieldMenuMaxHeight = 320.dp

/** Approximate height of a single-line option row (Material 3 menu item min height). */
private val AppSelectOptionRowHeight = 48.dp

/** Approximate height of a two-line option row (label + supporting text). */
private val AppSelectOptionRowHeightWithSupport = 56.dp

/** Options shown before the dropdown scrolls; the next one peeks to hint scrollability. */
private const val MAX_VISIBLE_SELECT_OPTIONS = 5

/**
 * One entry in an [AppSelectField] dropdown.
 *
 * @param value payload returned via `onSelect` when this row is chosen.
 * @param label primary text shown in the anchor field and the menu row.
 * @param supportingText optional secondary line shown under [label] in the menu row.
 * @param icon optional leading icon for the menu row.
 */
public data class AppSelectOption<T>(
    val value: T,
    val label: String,
    val supportingText: String? = null,
    val icon: ImageVector? = null,
)

/**
 * A generic Material 3 "select box": a read-only [OutlinedTextField] anchor that opens an
 * exposed dropdown of [options]. Its outlined look mirrors the other form fields so it sits
 * naturally in settings sheets and forms.
 *
 * The dropdown popup follows the Cosmos design — a translucent canopy-glass panel with an
 * ember→violet gradient hairline border (mirroring `canopyGlass`) and the active option
 * accent-highlighted (tinted row, accent label, and check).
 *
 * The current selection is driven by [selected] (matched against [AppSelectOption.value]); pass
 * `null` to show [placeholder] — e.g. a "Custom" state where the bound values match no option.
 *
 * The dropdown shows up to [MAX_VISIBLE_SELECT_OPTIONS] options — peeking the next as a scroll
 * hint — and never grows past [menuMaxHeight] nor the on-screen height (the popup clamps to it);
 * beyond that it scrolls with the shared fade/arrow [ScrollEdgeEffect].
 *
 * @param options entries to display, in order.
 * @param selected currently-selected value, or `null` for none.
 * @param onSelect invoked with the chosen option's value.
 * @param label floating label for the anchor field.
 * @param modifier applied to the [ExposedDropdownMenuBox] container.
 * @param placeholder shown in the anchor when [selected] is `null` or matches no option.
 * @param leadingIcon optional leading icon on the anchor field.
 * @param enabled whether the field can be opened.
 * @param supportingText optional supporting text under the anchor field.
 * @param menuMaxHeight upper bound on dropdown height; the list also caps near [MAX_VISIBLE_SELECT_OPTIONS] rows before scrolling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun <T> AppSelectField(
    options: List<AppSelectOption<T>>,
    selected: T?,
    onSelect: (T) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    supportingText: @Composable (() -> Unit)? = null,
    menuMaxHeight: Dp = AppSelectFieldMenuMaxHeight,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = remember(options, selected) { options.firstOrNull { it.value == selected } }

    val menuScrollState = rememberScrollState()
    // Cap the menu near [MAX_VISIBLE_SELECT_OPTIONS] rows with a half-row peek of the next, so a
    // longer list scrolls behind the shared fade/arrow affordance. Rows with supporting text are
    // taller, so pick the matching estimate; kept under [menuMaxHeight] (and the on-screen height,
    // which the popup clamps further).
    val optionRowHeight =
        if (options.any { it.supportingText != null }) AppSelectOptionRowHeightWithSupport else AppSelectOptionRowHeight
    val menuHeightCap = minOf(menuMaxHeight, optionRowHeight * MAX_VISIBLE_SELECT_OPTIONS + optionRowHeight / 2)

    val colorScheme = MaterialTheme.colorScheme
    val accentColor = colorScheme.primary
    // Cosmos "canopy glass" popup: a translucent surface + thin ember→violet gradient border,
    // mirroring canopyGlass(). Deliberately no Material shadow — the Cosmos language uses borders,
    // not elevation, for depth, and an elevation blur gets re-rasterized into the menu's alpha layer
    // on every frame of its open animation (the main source of dropdown jank). Remembered so the
    // brush + stroke are allocated once, not on every recomposition.
    val menuBorder =
        remember(accentColor, colorScheme.tertiary) {
            BorderStroke(
                width = 1.dp,
                brush =
                    Brush.linearGradient(
                        colors =
                            listOf(
                                accentColor.copy(alpha = 0.30f),
                                colorScheme.tertiary.copy(alpha = 0.21f),
                                accentColor.copy(alpha = 0.135f),
                            ),
                    ),
            )
        }
    val menuContainerColor = colorScheme.surface.copy(alpha = 0.97f)
    // Hoisted out of the option loop so a recomposition builds the two palettes once, not per row.
    val defaultItemColors = MenuDefaults.itemColors()
    val selectedItemColors =
        MenuDefaults.itemColors(
            textColor = accentColor,
            leadingIconColor = accentColor,
            trailingIconColor = accentColor,
        )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedOption?.label.orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            leadingIcon =
                leadingIcon?.let {
                    {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            supportingText = supportingText,
            singleLine = true,
            modifier =
                Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                    .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.large,
            containerColor = menuContainerColor,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = menuBorder,
        ) {
            ScrollEdgeEffect(
                state = menuScrollState,
                orientation = Orientation.Vertical,
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = menuHeightCap)
                            .verticalScroll(menuScrollState),
                ) {
                    options.forEach { option ->
                        val isSelected = option.value == selected
                        DropdownMenuItem(
                            modifier =
                                if (isSelected) {
                                    Modifier.background(accentColor.copy(alpha = 0.12f))
                                } else {
                                    Modifier
                                },
                            text = {
                                Column {
                                    Text(text = option.label, style = MaterialTheme.typography.bodyLarge)
                                    option.supportingText?.let { supporting ->
                                        Text(
                                            text = supporting,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            },
                            onClick = {
                                expanded = false
                                onSelect(option.value)
                            },
                            leadingIcon =
                                option.icon?.let {
                                    { Icon(imageVector = it, contentDescription = null) }
                                },
                            trailingIcon =
                                if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = stringResource(Res.string.simt_ui_cd_selected),
                                        )
                                    }
                                } else {
                                    null
                                },
                            colors = if (isSelected) selectedItemColors else defaultItemColors,
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }
        }
    }
}
