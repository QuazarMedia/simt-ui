package app.simt.ui.components.forms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.simt.ui.effects.LongPressTooltipEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun <T> IconColorNameRow(
    name: String,
    onNameChange: (String) -> Unit,
    selectedIcon: T,
    onIconChange: (T) -> Unit,
    selectedColor: Color,
    onColorChange: (Color) -> Unit,
    nameLabel: String,
    iconSelectorContentDescription: String,
    colorSelectorContentDescription: String,
    modifier: Modifier = Modifier,
    rowModifier: Modifier = Modifier,
    nameModifier: Modifier = Modifier,
    nameMaxLength: Int = 64,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null,
    availableIcons: List<T>,
    availableColors: List<Color>,
    colorContentDescription: ((Color) -> String)? = null,
    nameTestTag: String? = null,
    isIconPaletteExpanded: Boolean? = null,
    onIconPaletteExpandedChange: ((Boolean) -> Unit)? = null,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    iconContent: @Composable (T, Dp) -> Unit,
) {
    var internalIconPaletteExpanded by remember { mutableStateOf(isIconPaletteExpanded ?: false) }
    val iconPaletteExpanded = isIconPaletteExpanded ?: internalIconPaletteExpanded
    var colorPaletteExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(isIconPaletteExpanded) {
        if (isIconPaletteExpanded != null) {
            internalIconPaletteExpanded = isIconPaletteExpanded
        }
    }

    val setIconPaletteExpanded: (Boolean) -> Unit = { expanded ->
        internalIconPaletteExpanded = expanded
        onIconPaletteExpandedChange?.invoke(expanded)
        if (expanded) colorPaletteExpanded = false
    }

    val iconChipBg =
        if (isDarkTheme) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    val iconChipBorder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val activeChipBorder = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    val innerShadowColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
    val chipShape = MaterialTheme.shapes.large

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = rowModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconPickerChip(
                expanded = iconPaletteExpanded,
                onClick = { setIconPaletteExpanded(!iconPaletteExpanded) },
                contentDescription = iconSelectorContentDescription,
                isDarkTheme = isDarkTheme,
            ) {
                iconContent(selectedIcon, 26.dp)
            }

            Spacer(Modifier.width(10.dp))

            LongPressTooltipEffect(
                tooltipText = colorSelectorContentDescription,
                onClick = {
                    colorPaletteExpanded = !colorPaletteExpanded
                    if (colorPaletteExpanded) setIconPaletteExpanded(false)
                },
                modifier = Modifier.clip(chipShape),
                fillMaxHeight = false,
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier =
                        Modifier
                            .size(56.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = colorSelectorContentDescription
                            }
                            .let {
                                if (isDarkTheme) {
                                    it.subtleInnerShadow(shape = chipShape, color = innerShadowColor)
                                } else {
                                    it
                                }
                            },
                    shape = chipShape,
                    color = iconChipBg,
                    border =
                        BorderStroke(
                            2.dp,
                            if (colorPaletteExpanded) activeChipBorder else iconChipBorder,
                        ),
                    tonalElevation = 2.dp,
                    shadowElevation = 1.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.size(22.dp)) {
                            drawCircle(color = selectedColor)
                        }
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            ClearableTextField(
                value = name,
                onValueChange = { onNameChange(it.take(nameMaxLength)) },
                label = nameLabel,
                modifier = Modifier.weight(1f).then(nameModifier),
                maxLength = nameMaxLength,
                isError = isError,
                supportingText = supportingText,
                testTag = nameTestTag,
            )
        }

        AnimatedVisibility(visible = iconPaletteExpanded) {
            IconPickerPalette(
                options = availableIcons,
                selected = selectedIcon,
                onSelected = onIconChange,
                isDarkTheme = isDarkTheme,
                icon = iconContent,
            )
        }

        AnimatedVisibility(visible = colorPaletteExpanded) {
            IconPickerPalette(
                options = availableColors,
                selected = selectedColor,
                onSelected = onColorChange,
                glyphSize = 18.dp,
                isDarkTheme = isDarkTheme,
                optionContentDescription = colorContentDescription,
            ) { color, size ->
                Canvas(modifier = Modifier.size(size)) {
                    drawCircle(color = color)
                }
            }
        }
    }
}
