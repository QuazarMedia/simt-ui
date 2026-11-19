package app.simt.ui.components.permissions

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Enum of Animated Dialog Background Style values.
 *
 * Retained for call-site compatibility — in the shared library every style renders the same
 * injected [AnimatedDialogBackground.background] renderer. The only behavioral difference is
 * accent resolution: [NetworkGrid] selects [AnimatedDialogBackground.secondaryColor] as the
 * accent, every other style selects [AnimatedDialogBackground.primaryColor].
 */
public enum class AnimatedDialogBackgroundStyle {
    NetworkGrid,
    RadarSweep,
    Geometric,
}

/**
 * Decorative animated header background for dialogs and launch gates.
 *
 * The actual pixels are owned by the caller through [background]: it receives the accent color
 * resolved from [style]/[primaryColor]/[secondaryColor] and the [frozen] flag, and renders
 * whatever ambient background the application has (in simt-app this is the Cosmos gate
 * background). Performance tiering (animation vs. fully static) is the renderer's concern.
 *
 * @param style Legacy style key; only affects accent resolution (see above).
 * @param primaryColor Accent for the non-[NetworkGrid] styles.
 * @param secondaryColor Accent for the [NetworkGrid] style.
 * @param frozen Passed through to [background]; renderers use it to freeze ambient animation.
 */
@Composable
public fun AnimatedDialogBackground(
    style: AnimatedDialogBackgroundStyle,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.tertiary,
    frozen: Boolean = false,
    background: @Composable (accentColor: Color, frozen: Boolean) -> Unit,
) {
    val accent = if (style == AnimatedDialogBackgroundStyle.NetworkGrid) secondaryColor else primaryColor
    Box(modifier = modifier) { background(accent, frozen) }
}
