package app.simt.ui.effects

import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.simt.ui.components.banners.InformationBanner
import app.simt.ui.components.dialogs.ModalDefaults
import app.simt.ui.components.loader.BlockingLoaderController
import app.simt.ui.internal.platform.findActivity
import app.simt.ui.resources.Res
import app.simt.ui.resources.simt_ui_blocking_loader_background_processing_warning
import app.simt.ui.resources.simt_ui_cancel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Renders the global blocking loader above everything using a [Dialog], so it also overlays other
 * dialogs/modals/drawers.
 */
@Composable
public fun GlobalBlockingLoaderHost(
    controller: BlockingLoaderController,
    modifier: Modifier = Modifier,
) {
    val state by controller.uiState.collectAsStateWithLifecycle()

    // Keep screen on while blocking loader is visible
    val context = LocalContext.current
    val fallbackView = LocalView.current
    val activity = context.findActivity()
    val dialogWindow = (fallbackView.parent as? DialogWindowProvider)?.window
    val window = dialogWindow ?: activity?.window
    val targetView = window?.decorView ?: fallbackView

    DisposableEffect(state.visible, window, targetView) {
        val initialKeepScreenOnView = targetView.keepScreenOn
        val initialKeepScreenOnFlagSet =
            window?.attributes
                ?.flags
                ?.and(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                ?.let { it != 0 }
                ?: false

        if (state.visible) {
            targetView.keepScreenOn = true
            if (window != null && !initialKeepScreenOnFlagSet) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        onDispose {
            if (state.visible) {
                targetView.keepScreenOn = initialKeepScreenOnView
                if (window != null && !initialKeepScreenOnFlagSet) {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }
    }

    if (!state.visible) return

    Dialog(
        onDismissRequest = { /* blocked */ },
        properties =
            ModalDefaults.fullScreenDialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
    ) {
        var isInterrupting by remember { mutableStateOf(false) }
        var showBackgroundWarningBanner by
            remember(state.backgroundProcessingWarningMessage, state.showBackgroundProcessingWarning) {
                mutableStateOf(true)
            }
        val coroutineScope = rememberCoroutineScope()

        Box(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { /* Consume all input */ },
            contentAlignment = Alignment.Center,
        ) {
            // Close button in center top, respecting safe margins
            if (state.onInterrupt != null) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(top = 16.dp),
                ) {
                    IconButton(
                        onClick = {
                            if (!isInterrupting) {
                                isInterrupting = true
                                coroutineScope.launch {
                                    val success = controller.requestInterrupt()
                                    if (success) {
                                        // Interrupt successful, loader will be hidden by the component
                                        isInterrupting = false
                                    } else {
                                        // Interrupt failed, wait for process to complete
                                        isInterrupting = false
                                    }
                                }
                            }
                        },
                        modifier =
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Red.copy(alpha = 0.9f)),
                        enabled = !isInterrupting,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(Res.string.simt_ui_cancel),
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 32.dp),
            ) {
                val progress = state.progress
                if (progress != null) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier =
                            Modifier
                                .fillMaxWidth(0.7f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        strokeCap = StrokeCap.Round,
                    )
                } else {
                    CircularProgressIndicator(color = Color.White)
                }

                val message = state.message
                if (!message.isNullOrBlank()) {
                    Box(
                        modifier =
                            Modifier
                                .padding(top = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.9f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // Warning card at the bottom center
            if (state.showBackgroundProcessingWarning && showBackgroundWarningBanner) {
                val isBackgroundInfoBanner = !state.backgroundProcessingWarningMessage.isNullOrBlank()
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 32.dp, start = 32.dp, end = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    InformationBanner(
                        text =
                            state.backgroundProcessingWarningMessage
                                ?: stringResource(Res.string.simt_ui_blocking_loader_background_processing_warning),
                        icon = if (isBackgroundInfoBanner) Icons.Default.Info else Icons.Default.Warning,
                        tintColor =
                            if (isBackgroundInfoBanner) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        backgroundColor =
                            if (isBackgroundInfoBanner) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
                            } else {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                            },
                        onClose = { showBackgroundWarningBanner = false },
                    )
                }
            }
        }
    }
}
