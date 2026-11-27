package app.simt.ui.components.backgrounds

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.simt.ui.components.permissions.AnimatedDialogBackgroundStyle

/**
 * Cosmos gate background for dialogs and launch gates, with the caller's accent policy.
 *
 * [NetworkGrid] uses [secondaryColor]; all other styles use [primaryColor]. Ambient animation,
 * heading parallax and performance tiering are resolved by [CosmosBackground].
 */
@Composable
public fun AnimatedDialogBackground(
    style: AnimatedDialogBackgroundStyle,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.tertiary,
    frozen: Boolean = false,
) {
    val accent = if (style == AnimatedDialogBackgroundStyle.NetworkGrid) secondaryColor else primaryColor
    CosmosBackground(
        modifier = modifier,
        variant = CosmosVariant.Gate,
        accentColor = accent,
        frozen = frozen,
    )
}

/** Full-size Cosmos gate renderer for permission and feature gate header slots. */
@Composable
public fun gateHeaderBackground(
    accentColor: Color,
    frozen: Boolean = false,
) {
    CosmosBackground(
        modifier = Modifier.fillMaxSize(),
        variant = CosmosVariant.Gate,
        accentColor = accentColor,
        frozen = frozen,
    )
}
