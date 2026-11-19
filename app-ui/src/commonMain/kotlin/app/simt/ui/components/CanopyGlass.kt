package app.simt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * The Cosmos "canopy glass" surface treatment (docs/design/COSMOS_REDESIGN.md §3):
 * a translucent warm panel with a thin ember→violet gradient border.
 *
 * Deliberately avoids real-time blur — translucency plus a gradient border reads as glass at a
 * fraction of the GPU cost, keeping low-end devices smooth.
 *
 * @param shape Panel shape (defaults to the theme's large shape).
 * @param surfaceAlpha Background translucency; raise it when legibility matters more than depth.
 * @param borderAlpha Strength of the gradient border; 0f removes the border entirely.
 * @param accentColor Optional override for the border's leading color (defaults to primary).
 */
@Composable
public fun Modifier.canopyGlass(
    shape: Shape = MaterialTheme.shapes.large,
    surfaceAlpha: Float = 0.90f,
    borderAlpha: Float = 0.30f,
    accentColor: Color? = null,
): Modifier {
    val colorScheme = MaterialTheme.colorScheme
    val leading = accentColor ?: colorScheme.primary
    val borderBrush =
        remember(leading, colorScheme.tertiary, borderAlpha) {
            Brush.linearGradient(
                colors =
                    listOf(
                        leading.copy(alpha = borderAlpha),
                        colorScheme.tertiary.copy(alpha = borderAlpha * 0.7f),
                        leading.copy(alpha = borderAlpha * 0.45f),
                    ),
            )
        }
    val background = colorScheme.surface.copy(alpha = surfaceAlpha)
    return this
        .clip(shape)
        .background(background)
        .let { if (borderAlpha > 0f) it.border(1.dp, borderBrush, shape) else it }
}
