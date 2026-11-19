package app.simt.ui.components.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.simt.ui.components.canopyGlass
import app.simt.ui.effects.ScrollEdgeEffect
import app.simt.ui.internal.platform.currentViewParent
import app.simt.ui.internal.platform.suppressDialogWindowAnimations
import app.simt.ui.resources.Res
import app.simt.ui.resources.simt_ui_close
import org.jetbrains.compose.resources.stringResource

/**
 * Default properties for centered, edge-to-edge dialogs in SIMT.
 */
public object ModalDefaults {
    public val FullScreenDialogProperties: DialogProperties =
        DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        )

    public fun fullScreenDialogProperties(
        dismissOnBackPress: Boolean = true,
        dismissOnClickOutside: Boolean = true,
    ): DialogProperties =
        DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        )
}

/**
 * A reusable dialog component that displays content in a scrollable card. The dialog sizes to
 * content height but won't exceed screen height.
 *
 * It uses custom AnimatedVisibility for transitions and disables the default platform dialog
 * animations to provide a smooth, unified "navigation-like" experience.
 *
 * @param onDismissRequest Callback when dialog should be dismissed
 * @param modifier Modifier for the dialog
 * @param dismissOnBackPress Whether to dismiss on back press (default: true)
 * @param dismissOnClickOutside Whether to dismiss when clicking outside (default: true)
 * @param wrapInDialog Whether to wrap in a platform Dialog window
 * @param overlay Optional overlay content to display on top of the dialog (e.g., toasts)
 * @param header Optional header content
 * @param content Main content
 */
@Composable
public fun ContentDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier.Companion,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    wrapInDialog: Boolean = true,
    overlay: (@Composable () -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    // In @Preview/inspection the LaunchedEffect below never runs, so seed the target state to
    // visible up-front — the enter animation then resolves immediately and the card renders.
    val inInspectionMode = LocalInspectionMode.current
    val transitionState = remember { MutableTransitionState(false).apply { if (inInspectionMode) targetState = true } }
    var pendingDismiss by remember { mutableStateOf<Boolean>(false) }

    fun requestDismiss() {
        if (!pendingDismiss) {
            pendingDismiss = true
            // Call onDismissRequest immediately to match "Back" behavior and avoid
            // local animation delays that cause MapView freezing.
            onDismissRequest()
        }
    }

    LaunchedEffect(Unit) {
        transitionState.targetState = true
    }

    @Composable
    fun DialogContentInternal() {
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp.dp
        val screenHeightDp = configuration.screenHeightDp.dp

        // Max width = min(width, height) for better UI on wide screens (e.g. tablets landscape)
        val maxContentWidth = min(screenWidthDp, screenHeightDp) - 32.dp

        // Calculate safe height
        val safeDrawingPadding = WindowInsets.Companion.safeDrawing.asPaddingValues()
        val topInset = safeDrawingPadding.calculateTopPadding()
        val bottomInset = safeDrawingPadding.calculateBottomPadding()
        val maxCardHeight = screenHeightDp - topInset - bottomInset - 32.dp

        // Disable platform animations if we are in a Dialog window
        val viewParent = currentViewParent()
        SideEffect { suppressDialogWindowAnimations(viewParent) }

        Box(
            modifier =
                modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .clickable(
                        enabled = dismissOnClickOutside && !pendingDismiss,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { requestDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            // Unified transition: 400ms Enter (Fade), 200ms Exit (Fade)
            AnimatedVisibility(
                visibleState = transitionState,
                enter = fadeIn(animationSpec = tween(400)),
                exit = fadeOut(animationSpec = tween(200)),
            ) {
                Card(
                    modifier =
                        Modifier
                            .widthIn(max = maxContentWidth)
                            .fillMaxWidth()
                            .heightIn(max = maxCardHeight)
                            .padding(8.dp)
                            .canopyGlass(shape = MaterialTheme.shapes.large, surfaceAlpha = 0.96f)
                            .clickable(
                                indication = null,
                                interactionSource =
                                    remember { MutableInteractionSource() },
                            ) {},
                    shape = MaterialTheme.shapes.large,
                    // canopyGlass paints the surface itself; the Card just needs the
                    // matching content color so text doesn't fall back to black.
                    colors =
                        CardDefaults.cardColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Box(
                        modifier = Modifier.clip(MaterialTheme.shapes.large),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            header?.invoke()
                            val contentScrollState = rememberScrollState()
                            ScrollEdgeEffect(
                                state = contentScrollState,
                                orientation = Orientation.Vertical,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 0.dp)
                                            .verticalScroll(contentScrollState),
                                ) {
                                    content()
                                }
                            }
                        }

                        // Close button
                        Box(
                            modifier =
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .clickable(enabled = !pendingDismiss) { requestDismiss() },
                        ) {
                            IconButton(
                                onClick = { requestDismiss() },
                                enabled = !pendingDismiss,
                                modifier =
                                    Modifier
                                        .background(
                                            color =
                                                MaterialTheme.colorScheme.surfaceVariant.copy(
                                                    alpha = 0.5f,
                                                ),
                                            shape = RoundedCornerShape(50.dp),
                                        )
                                        .size(32.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(Res.string.simt_ui_close),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }

            overlay?.invoke()
        }
    }

    if (wrapInDialog) {
        Dialog(
            onDismissRequest = { requestDismiss() },
            properties =
                ModalDefaults.fullScreenDialogProperties(
                    dismissOnBackPress = dismissOnBackPress,
                    dismissOnClickOutside = dismissOnClickOutside,
                ),
            content = { DialogContentInternal() },
        )
    } else {
        DialogContentInternal()
    }
}
