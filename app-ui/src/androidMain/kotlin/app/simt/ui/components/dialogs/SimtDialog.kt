package app.simt.ui.components.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import app.simt.ui.components.GradientDivider
import app.simt.ui.components.canopyGlass
import app.simt.ui.effects.LocalCosmosTier
import app.simt.ui.effects.SimtMotion
import app.simt.ui.resources.Res
import app.simt.ui.resources.simt_ui_cancel
import org.jetbrains.compose.resources.stringResource

/**
 * The two centered-panel layouts served by [SimtDialog].
 *
 * - [Alert] — Material 3 `AlertDialog`-style panel: optional icon halo, title, styled body, a
 *   [GradientDivider] hairline, and a trailing action row (confirm/dismiss).
 * - [Glass] — a plain glass panel hosting fully custom, centered content with an optional close
 *   affordance and no imposed action row (for animated/creation flows).
 */
public enum class SimtDialogVariant {
    Alert,
    Glass,
}

/**
 * Canonical centered glass dialog surface (Layer 1) — the single source of truth for the app's
 * centered panel dialogs. It owns the modal mechanics once (platform/nav window hosting, window
 * animation suppression, tier-aware entrance/exit) and renders one of the [SimtDialogVariant]
 * panels on top.
 *
 * Prefer the Layer-2 presets ([ConfirmDialog], [PromptDialog], [ActionListDialog], …) over calling
 * this directly; reach for `SimtDialog` when you need a bespoke panel.
 *
 * @param accentColor Leading color for the glass border gradient and (Alert) icon halo. Defaults
 *   to the theme primary.
 * @param onClose Glass-variant only: renders a small circular close (X) at the top-end corner.
 * @param wrapInDialog When true (default) the panel hosts its own platform [Dialog] window. Set to
 *   false when already hosted by a Navigation Compose `dialog<>` destination to avoid a double scrim.
 * @param usePlatformDefaultWidth When false, callers can size the panel relative to the full window
 *   through [modifier].
 */
@Composable
public fun SimtDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    variant: SimtDialogVariant = SimtDialogVariant.Alert,
    confirmButton: (@Composable () -> Unit)? = null,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable ColumnScope.() -> Unit)? = null,
    accentColor: Color? = null,
    onClose: (() -> Unit)? = null,
    wrapInDialog: Boolean = true,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    usePlatformDefaultWidth: Boolean = true,
) {
    CosmosDialogWindow(
        wrapInDialog = wrapInDialog,
        onDismissRequest = onDismissRequest,
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = dismissOnClickOutside,
        usePlatformDefaultWidth = usePlatformDefaultWidth,
    ) {
        when (variant) {
            SimtDialogVariant.Alert ->
                AlertPanel(
                    modifier = modifier,
                    confirmButton = confirmButton,
                    dismissButton = dismissButton,
                    icon = icon,
                    title = title,
                    text = text,
                    accentColor = accentColor,
                )

            SimtDialogVariant.Glass ->
                GlassPanel(
                    modifier = modifier,
                    accent = accentColor,
                    onClose = onClose,
                    content = text ?: {},
                )
        }
    }
}

/**
 * Shared modal mechanics for every [SimtDialog] variant: optional platform [Dialog] hosting, a
 * suppressed platform/nav window animation, and the tier-aware Cosmos scale+fade entrance.
 */
@Composable
private fun CosmosDialogWindow(
    wrapInDialog: Boolean,
    onDismissRequest: () -> Unit,
    dismissOnBackPress: Boolean,
    dismissOnClickOutside: Boolean,
    usePlatformDefaultWidth: Boolean,
    content: @Composable () -> Unit,
) {
    if (wrapInDialog) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties =
                DialogProperties(
                    dismissOnBackPress = dismissOnBackPress,
                    dismissOnClickOutside = dismissOnClickOutside,
                    usePlatformDefaultWidth = usePlatformDefaultWidth,
                ),
        ) {
            CosmosDialogEntrance(content)
        }
    } else {
        CosmosDialogEntrance(content)
    }
}

@Composable
private fun CosmosDialogEntrance(content: @Composable () -> Unit) {
    // The Cosmos entrance below replaces the platform window animation (also suppresses the host
    // nav dialog's default window animation when wrapInDialog is false).
    val view = LocalView.current
    SideEffect {
        (view.parent as? DialogWindowProvider)?.window?.setWindowAnimations(0)
    }

    val tier = LocalCosmosTier.current
    val transitionState =
        remember {
            // STATIC tier starts visible: zero animation cost on low-end devices.
            MutableTransitionState(!tier.allowsEntranceAnimations).apply { targetState = true }
        }

    AnimatedVisibility(
        visibleState = transitionState,
        enter =
            fadeIn(SimtMotion.enterSpec()) +
                scaleIn(initialScale = 0.92f, animationSpec = SimtMotion.enterSpec()),
        exit = fadeOut(SimtMotion.exitSpec()),
    ) {
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlertPanel(
    modifier: Modifier,
    confirmButton: (@Composable () -> Unit)?,
    dismissButton: (@Composable () -> Unit)?,
    icon: (@Composable () -> Unit)?,
    title: (@Composable () -> Unit)?,
    text: (@Composable ColumnScope.() -> Unit)?,
    accentColor: Color?,
) {
    val colorScheme = MaterialTheme.colorScheme
    val accent = accentColor ?: colorScheme.primary
    val haloBrush =
        remember(accent) {
            Brush.radialGradient(
                colors =
                    listOf(
                        accent.copy(alpha = 0.28f),
                        accent.copy(alpha = 0.10f),
                        Color.Transparent,
                    ),
            )
        }

    Column(
        modifier =
            modifier
                .sizeIn(minWidth = 280.dp, maxWidth = 560.dp)
                .canopyGlass(
                    shape = MaterialTheme.shapes.extraLarge,
                    surfaceAlpha = 0.96f,
                    accentColor = accent,
                )
                .padding(horizontal = 24.dp, vertical = 22.dp),
    ) {
        // Anchor content color explicitly: the glass panel is painted by canopyGlass,
        // so nothing here should fall back to contentColorFor(Transparent).
        CompositionLocalProvider(LocalContentColor provides colorScheme.onSurface) {
            if (icon != null) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(56.dp)
                            .background(haloBrush, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    CompositionLocalProvider(LocalContentColor provides accent) {
                        icon()
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (title != null) {
                Box(
                    modifier =
                        if (icon != null) {
                            Modifier.align(Alignment.CenterHorizontally)
                        } else {
                            Modifier.fillMaxWidth()
                        },
                ) {
                    ProvideTextStyle(MaterialTheme.typography.titleLarge) {
                        title()
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (text != null) {
                // Weighted (non-filling) so tall bodies compress instead of pushing
                // the actions off-screen — mirrors M3 AlertDialog behavior.
                Column(modifier = Modifier.weight(weight = 1f, fill = false)) {
                    CompositionLocalProvider(
                        LocalContentColor provides colorScheme.onSurfaceVariant,
                    ) {
                        ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                            text()
                        }
                    }
                }
            }

            GradientDivider(
                modifier = Modifier.padding(top = 16.dp, bottom = 10.dp),
                alpha = 0.35f,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                dismissButton?.invoke()
                confirmButton?.invoke()
            }
        }
    }
}

@Composable
private fun GlassPanel(
    modifier: Modifier,
    accent: Color?,
    onClose: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    val resolvedAccent = accent ?: MaterialTheme.colorScheme.primary
    Box(
        modifier =
            modifier
                .sizeIn(minWidth = 300.dp, maxWidth = 420.dp)
                .canopyGlass(
                    shape = MaterialTheme.shapes.extraLarge,
                    surfaceAlpha = 0.97f,
                    accentColor = resolvedAccent,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            // Fill the panel width so children actually center across the full dialog
            // (without this the wrapped column hugs the start when content is narrow).
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                content()
            }
        }
        if (onClose != null) {
            IconButton(
                onClick = onClose,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = CircleShape,
                        ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(Res.string.simt_ui_cancel),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
