package app.simt.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Cosmos "command deck" section rule: a hairline that fades in from the edges through a warm
 * primary->tertiary gradient instead of a flat divider. Used to separate sections in drawers
 * and panels (docs/design/COSMOS_REDESIGN.md §4).
 */
@Composable
public fun GradientDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    alpha: Float = 0.45f,
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val brush =
        remember(primary, tertiary, alpha) {
            Brush.horizontalGradient(
                colors =
                    listOf(
                        Color.Transparent,
                        primary.copy(alpha = alpha),
                        tertiary.copy(alpha = alpha),
                        Color.Transparent,
                    ),
            )
        }
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(thickness)
                .drawBehind { drawRect(brush = brush) },
    )
}
