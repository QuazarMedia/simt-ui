package app.simt.ui.components.forms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.simt.ui.effects.LongPressTooltipEffect
import app.simt.ui.effects.ScrollEdgeEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun IconPickerChip(
    expanded: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val chipBg =
        if (isDarkTheme) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    val chipBorder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val activeBorder = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    val innerShadowColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
    val chipShape = MaterialTheme.shapes.large

    LongPressTooltipEffect(
        tooltipText = contentDescription,
        onClick = onClick,
        modifier = modifier.clip(chipShape),
        fillMaxHeight = false,
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier =
                Modifier
                    .size(56.dp)
                    .semantics {
                        role = Role.Button
                        this.contentDescription = contentDescription
                    }
                    .let {
                        if (isDarkTheme) {
                            it.subtleInnerShadow(shape = chipShape, color = innerShadowColor)
                        } else {
                            it
                        }
                    },
            shape = chipShape,
            color = chipBg,
            border = BorderStroke(2.dp, if (expanded) activeBorder else chipBorder),
            tonalElevation = 2.dp,
            shadowElevation = 1.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                content()
            }
        }
    }
}

private val PaletteItemSize = 36.dp
private val PaletteItemSpacing = 10.dp

@Composable
public fun <T> IconPickerPalette(
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    glyphSize: Dp = 20.dp,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    optionContentDescription: ((T) -> String)? = null,
    icon: @Composable (T, Dp) -> Unit,
) {
    val chipBg =
        if (isDarkTheme) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    val chipBorder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val activeBorder = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    val chipShape = MaterialTheme.shapes.large

    Column(modifier = modifier.fillMaxWidth().padding(top = 12.dp)) {
        val listState = rememberLazyListState()
        val density = LocalDensity.current
        // The reveal starts at zero size, so retry until the selected item is measurable.
        LaunchedEffect(Unit) {
            val selectedIndex = options.indexOf(selected).coerceAtLeast(0)
            val inset = with(density) { 24.dp.roundToPx() }
            repeat(60) {
                val layoutInfo = listState.layoutInfo
                if (
                    layoutInfo.totalItemsCount > 0 &&
                    layoutInfo.visibleItemsInfo.any { it.index == selectedIndex }
                ) {
                    return@LaunchedEffect
                }
                listState.scrollToItem(selectedIndex, -inset)
                withFrameNanos { }
            }
        }
        ScrollEdgeEffect(
            state = listState,
            orientation = Orientation.Horizontal,
            modifier = Modifier.fillMaxWidth(),
            fadeSize = 28.dp,
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(PaletteItemSpacing),
            ) {
                items(options) { option ->
                    val isSelected = option == selected
                    val description = optionContentDescription?.invoke(option)
                    Surface(
                        modifier =
                            Modifier.size(PaletteItemSize).then(
                                if (description != null) {
                                    Modifier.semantics { contentDescription = description }
                                } else {
                                    Modifier
                                },
                            ),
                        shape = chipShape,
                        color = chipBg,
                        border = BorderStroke(2.dp, if (isSelected) activeBorder else chipBorder),
                        tonalElevation = if (isSelected) 3.dp else 1.dp,
                        shadowElevation = if (isSelected) 1.dp else 0.dp,
                        onClick = { onSelected(option) },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            icon(option, glyphSize)
                            if (isSelected) {
                                Canvas(modifier = Modifier.size(32.dp)) {
                                    drawCircle(
                                        color = activeBorder,
                                        style = Stroke(width = 5.dp.toPx()),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun Modifier.subtleInnerShadow(
    shape: Shape,
    color: Color,
): Modifier =
    this
        .clip(shape)
        .drawWithCache {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val brush =
                Brush.radialGradient(
                    colors = listOf(Color.Transparent, color),
                    center = center,
                    radius = radius,
                )
            onDrawWithContent {
                drawContent()
                drawRect(brush = brush)
            }
        }
