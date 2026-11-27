package app.simt.ui.components.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Immutable
public data class IconLabelActionsToolbarColors(
    val containerColor: Color,
    val edgeFadeColor: Color,
    val edgeFadeAlpha: Float,
    val dividerColor: Color,
)

public object IconLabelActionsToolbarDefaults {
    @Composable
    public fun colors(
        containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        edgeFadeColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        edgeFadeAlpha: Float = 0.6f,
        dividerColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    ): IconLabelActionsToolbarColors =
        remember(containerColor, edgeFadeColor, edgeFadeAlpha, dividerColor) {
            IconLabelActionsToolbarColors(
                containerColor = containerColor,
                edgeFadeColor = edgeFadeColor,
                edgeFadeAlpha = edgeFadeAlpha,
                dividerColor = dividerColor,
            )
        }
}

@Composable
public fun IconLabelActionsToolbar(
    actions: List<IconLabelAction>,
    modifier: Modifier = Modifier,
    colors: IconLabelActionsToolbarColors = IconLabelActionsToolbarDefaults.colors(),
    prefixContent: (@Composable () -> Unit)? = null,
    suffixContent: (@Composable () -> Unit)? = null,
    height: Dp = 56.dp,
    tonalElevation: Dp = 6.dp,
    shape: Shape? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp),
    itemSpacing: Dp = 8.dp,
    showOverflowIndicators: Boolean = true,
    isRedMode: Boolean = false,
    fixedEdgePadding: PaddingValues = PaddingValues(horizontal = 4.dp),
    showDividers: Boolean = true,
) {
    val layoutDirection = LocalLayoutDirection.current
    val effectiveShape =
        shape
            ?: if (prefixContent == null && suffixContent == null) {
                MaterialTheme.shapes.extraLarge.copy(CornerSize(10.dp))
            } else {
                MaterialTheme.shapes.extraLarge
            }
    val fade = colors.edgeFadeColor.copy(alpha = colors.edgeFadeAlpha)
    val leftEdgeGradientColors =
        remember(layoutDirection, fade) {
            if (layoutDirection == LayoutDirection.Ltr) {
                listOf(fade, Color.Transparent)
            } else {
                listOf(Color.Transparent, fade)
            }
        }
    val rightEdgeGradientColors =
        remember(layoutDirection, fade) {
            if (layoutDirection == LayoutDirection.Ltr) {
                listOf(Color.Transparent, fade)
            } else {
                listOf(fade, Color.Transparent)
            }
        }
    val borderPrimary = MaterialTheme.colorScheme.primary
    val borderTertiary = MaterialTheme.colorScheme.tertiary
    val borderBrush =
        remember(borderPrimary, borderTertiary) {
            Brush.linearGradient(
                colors =
                    listOf(
                        borderPrimary.copy(alpha = 0.30f),
                        borderTertiary.copy(alpha = 0.20f),
                    ),
            )
        }

    Surface(
        modifier = modifier.fillMaxWidth().border(1.dp, borderBrush, effectiveShape),
        tonalElevation = tonalElevation,
        shape = effectiveShape,
        color = colors.containerColor,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(height),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (prefixContent != null) {
                Box(
                    modifier = Modifier.background(Brush.horizontalGradient(leftEdgeGradientColors)),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        prefixContent()
                        if (showDividers) ToolbarVerticalDivider(color = colors.dividerColor)
                    }
                }
            }

            IconLabelButtonRow(
                actions = actions,
                modifier = Modifier.weight(1f),
                contentPadding = contentPadding,
                itemSpacing = itemSpacing,
                isRedMode = isRedMode,
                showOverflowIndicators = showOverflowIndicators,
            )

            if (suffixContent != null) {
                Box(
                    modifier =
                        Modifier
                            .background(Brush.horizontalGradient(rightEdgeGradientColors))
                            .padding(fixedEdgePadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (showDividers) ToolbarVerticalDivider(color = colors.dividerColor)
                        suffixContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolbarVerticalDivider(
    color: Color,
    width: Dp = 1.dp,
    height: Dp = 32.dp,
) {
    Spacer(
        modifier =
            Modifier
                .width(width)
                .height(height)
                .fillMaxHeight()
                .background(color),
    )
}
