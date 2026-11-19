package app.simt.ui.components.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.simt.ui.resources.Res
import app.simt.ui.resources.simt_ui_cancel
import app.simt.ui.resources.simt_ui_grant_permission
import org.jetbrains.compose.resources.stringResource

/**
 * The single, feature-agnostic rationale dialog for one runtime permission.
 *
 * It wraps the shared [PermissionGate] and derives all copy/styling from the
 * [PermissionPresentation], so it is the one dialog reused everywhere a runtime permission is
 * explained and requested. Applications own the mapping from their permission model onto the
 * presentation; no per-feature copy/icons are duplicated.
 *
 * @param presentation Theme/icon/copy for the permission being requested.
 * @param onDismiss Called when the dialog is dismissed without granting.
 * @param onGrantPermission Called when the user taps the grant button (host should launch the
 *   system prompt).
 * @param isGrantPermissionBlocked True when the OS won't show a prompt anymore ("Don't ask
 *   again"); the dialog then offers an "Open app settings" action instead.
 * @param onOpenAppSettings Called to deep-link into the app's system settings when blocked.
 * @param wrapInDialog Pass false when already hosted by a Navigation Compose `dialog<>`
 *   destination (single window / single scrim).
 * @param optionalContent Optional extra content rendered under the title (e.g. an active-links
 *   card). Kept generic via a slot so callers add context without a bespoke dialog.
 * @param headerBackground Renderer for the animated header background; see [PermissionGate].
 */
@Composable
public fun PermissionRequestDialog(
    presentation: PermissionPresentation,
    onDismiss: () -> Unit,
    onGrantPermission: () -> Unit,
    modifier: Modifier = Modifier,
    isGrantPermissionBlocked: Boolean = false,
    onOpenAppSettings: (() -> Unit)? = null,
    wrapInDialog: Boolean = true,
    optionalContent: (@Composable () -> Unit)? = null,
    headerBackground: @Composable (accentColor: Color, frozen: Boolean) -> Unit = { color, _ ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(color.copy(alpha = 0.12f)),
        )
    },
) {
    PermissionGate(
        title = androidx.compose.ui.res.stringResource(presentation.titleRes),
        description = "",
        icon = presentation.icon,
        iconTint = presentation.tint,
        backgroundStyle = presentation.backgroundStyle,
        onGetStarted = onGrantPermission,
        onCancel = onDismiss,
        modifier = modifier,
        permissionsExplanation = androidx.compose.ui.res.stringResource(presentation.explanationRes),
        optionalContent = optionalContent,
        wrapInDialog = wrapInDialog,
        // Keep this dialog's own button copy (grant/cancel) rather than the gate's get-started/back.
        getStartedButtonText = stringResource(Res.string.simt_ui_grant_permission),
        cancelButtonText = stringResource(Res.string.simt_ui_cancel),
        isBlocked = isGrantPermissionBlocked,
        onOpenAppSettings = onOpenAppSettings,
        getStartedContainerColor = presentation.buttonContainerColor,
        getStartedContentColor = presentation.buttonContentColor,
        headerBackground = headerBackground,
    )
}
