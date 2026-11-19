package app.simt.ui.components.backgrounds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Static warm gradient wash for bottom sheets and dialogs — the cheapest Cosmos surface
 * treatment (a single cached linear gradient, no animation, no stars).
 *
 * @param modifier Modifier for the background
 * @param primaryColor Primary gradient color (defaults to Material primary)
 * @param secondaryColor Secondary gradient color (defaults to Material tertiary)
 * @param accentColor Accent gradient color (defaults to Material secondary)
 */
@Composable
public fun GradientBackground(
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.tertiary,
    accentColor: Color = MaterialTheme.colorScheme.secondary,
) {
    // Cache gradient colour list — only changes when colour params change
    val gradientColors =
        remember(primaryColor, secondaryColor, accentColor) {
            listOf(
                primaryColor.copy(alpha = 0.08f),
                secondaryColor.copy(alpha = 0.06f),
                accentColor.copy(alpha = 0.04f),
                Color.Transparent,
            )
        }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        drawRect(
            brush =
                Brush.linearGradient(
                    colors = gradientColors,
                    start = Offset(0f, 0f),
                    end = Offset(width, height),
                ),
        )
    }
}
