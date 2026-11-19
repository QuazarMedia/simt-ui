package app.simt.ui.components.permissions

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.simt.ui.components.banners.InformationBanner
import app.simt.ui.components.buttons.CancelLinkButton
import app.simt.ui.components.dialogs.ContentDialog
import app.simt.ui.resources.Res
import app.simt.ui.resources.simt_ui_back
import app.simt.ui.resources.simt_ui_close
import app.simt.ui.resources.simt_ui_device_compatibility
import app.simt.ui.resources.simt_ui_get_started
import app.simt.ui.resources.simt_ui_open_app_settings
import app.simt.ui.resources.simt_ui_permission_request_blocked_hint
import org.jetbrains.compose.resources.stringResource

/**
 * The single "gate" shown before an experimental / permission-gated feature (Layer-2 preset). It
 * unifies the two former shells — the runtime-permission rationale dialog and the hardware
 * requirements gate — into one component so every gate in the app looks and behaves the same.
 *
 * It presents an animated header with the feature icon, a title + optional experimental tag +
 * description, and then adapts its body to the situation:
 * - **[requirements] present** → a device-compatibility row; the primary action is enabled only
 *   when all requirements are met, otherwise [requirementsNotMetText] is shown.
 * - **[isBlocked]** (the OS won't prompt anymore) → [blockedHint] plus an "open settings" action.
 * - **otherwise** → an optional [permissionsExplanation] banner and the primary action.
 *
 * @param title Feature/permission title.
 * @param description Short explanation of the feature and why access is needed (blank = hidden).
 * @param icon Feature icon shown in the header medallion.
 * @param iconTint Accent color driving the header, banner, and primary button.
 * @param backgroundStyle Animated header background style key (accent resolution only — see
 *   [AnimatedDialogBackground]).
 * @param headerBackground Renderer for the animated header background; receives the accent
 *   resolved from [backgroundStyle]/[iconTint] and the frozen flag. Defaults to a flat tinted
 *   wash; applications with an ambient background (e.g. simt-app's Cosmos gate background) pass
 *   their own renderer.
 * @param onGetStarted Primary action (grant permission / start feature).
 * @param onCancel Called on cancel/close, scrim tap, and back press.
 * @param experimental Whether to show the experimental tag under the title.
 * @param experimentalLabel Text for the experimental tag (defaults to the shared "experimental" copy).
 * @param requirements Hardware/software requirements; empty means "no compatibility gate".
 * @param permissionsExplanation Optional banner explaining the permissions requested.
 * @param optionalContent Optional extra content under the description (e.g. an active-links card).
 * @param wrapInDialog Pass false when already hosted by a Navigation Compose `dialog<>` destination.
 * @param isBlocked The OS won't show a prompt anymore ("Don't ask again") — offer settings instead.
 * @param blockedHint Message shown when [isBlocked].
 * @param onOpenAppSettings Deep-link into app settings, shown when [isBlocked].
 * @param getStartedContainerColor Optional override for the primary button container color.
 * @param getStartedContentColor Optional override for the primary button content color.
 */
@Composable
public fun PermissionGate(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    backgroundStyle: AnimatedDialogBackgroundStyle,
    onGetStarted: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    experimental: Boolean = false,
    experimentalLabel: String? = null,
    requirements: List<FeatureRequirement> = emptyList(),
    permissionsExplanation: String? = null,
    optionalContent: (@Composable () -> Unit)? = null,
    wrapInDialog: Boolean = true,
    getStartedButtonText: String = stringResource(Res.string.simt_ui_get_started),
    cancelButtonText: String = stringResource(Res.string.simt_ui_back),
    requirementsNotMetText: String = "This device does not meet the requirements for this feature.",
    isBlocked: Boolean = false,
    blockedHint: String = stringResource(Res.string.simt_ui_permission_request_blocked_hint),
    onOpenAppSettings: (() -> Unit)? = null,
    openAppSettingsButtonText: String = stringResource(Res.string.simt_ui_open_app_settings),
    getStartedContainerColor: Color? = null,
    getStartedContentColor: Color? = null,
    headerBackground: @Composable (accentColor: Color, frozen: Boolean) -> Unit = { color, _ ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(color.copy(alpha = 0.12f)),
        )
    },
) {
    val view = LocalView.current
    val allRequirementsMet = requirements.none { it.status == RequirementStatus.UNAVAILABLE }
    val isAnyChecking = requirements.any { it.status == RequirementStatus.CHECKING }
    var showPermissionsExplanation by rememberSaveable(permissionsExplanation) { mutableStateOf(true) }

    ContentDialog(
        onDismissRequest = onCancel,
        modifier = modifier,
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        wrapInDialog = wrapInDialog,
        header = {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(140.dp),
            ) {
                AnimatedDialogBackground(
                    style = backgroundStyle,
                    modifier = Modifier.fillMaxSize(),
                    primaryColor = iconTint,
                    secondaryColor = MaterialTheme.colorScheme.tertiary,
                    background = headerBackground,
                )

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.2f),
                        border = BorderStroke(2.dp, Color.White.copy(alpha = 0.5f)),
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .padding(16.dp)
                                    .fillMaxSize(),
                            tint = iconTint,
                        )
                    }
                }
            }
        },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )

                if (experimental && experimentalLabel != null) {
                    Text(
                        text = experimentalLabel,
                        style =
                            MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                color = iconTint,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                            ),
                        textAlign = TextAlign.Center,
                    )
                }

                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                    )
                }
            }

            optionalContent?.invoke()

            if (requirements.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.simt_ui_device_compatibility),
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                            ),
                        color = MaterialTheme.colorScheme.primary,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        requirements.forEach { requirement ->
                            FeatureRequirementCard(
                                modifier = Modifier.weight(1f),
                                requirement = requirement,
                            )
                        }
                    }
                }
            }

            when {
                isBlocked -> {
                    Text(
                        text = blockedHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (onOpenAppSettings != null) {
                        GateActionButton(
                            text = openAppSettingsButtonText,
                            onClick = onOpenAppSettings,
                            iconTint = iconTint,
                            containerColor = getStartedContainerColor,
                            contentColor = getStartedContentColor,
                        )
                    }
                }

                allRequirementsMet -> {
                    if (!permissionsExplanation.isNullOrBlank() && showPermissionsExplanation) {
                        InformationBanner(
                            text = permissionsExplanation,
                            icon = Icons.Default.Lock,
                            tintColor = iconTint,
                            backgroundColor = iconTint.copy(alpha = 0.1f),
                            onClose = { showPermissionsExplanation = false },
                        )
                    }
                    GateActionButton(
                        text = getStartedButtonText,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            onGetStarted()
                        },
                        iconTint = iconTint,
                        enabled = !isAnyChecking,
                        containerColor = getStartedContainerColor,
                        contentColor = getStartedContentColor,
                    )
                }

                else -> {
                    Text(
                        text = requirementsNotMetText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            CancelLinkButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                label =
                    if (allRequirementsMet || isBlocked) {
                        cancelButtonText
                    } else {
                        stringResource(Res.string.simt_ui_close)
                    },
            )
        }
    }
}

@Composable
private fun GateActionButton(
    text: String,
    onClick: () -> Unit,
    iconTint: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color? = null,
    contentColor: Color? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = containerColor ?: iconTint.copy(alpha = 0.5f),
                contentColor = contentColor ?: Color.White,
                disabledContainerColor = iconTint.copy(alpha = 0.2f),
                disabledContentColor = Color.White.copy(alpha = 0.5f),
            ),
        enabled = enabled,
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        Text(
            text = text.uppercase(),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
    }
}
