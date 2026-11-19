package app.simt.ui.components.toasts

import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.simt.ui.components.dialogs.ModalDefaults
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * Global toast overlay rendered above every screen — including open dialogs, modals and drawers.
 *
 * The stack is hosted inside its own full-screen [Dialog] window (mirroring the application's global
 * blocking-loader host) so it draws on top of any other [Dialog] that is currently open; an inline
 * overlay would be stuck behind those separate dialog windows. The toast window is made click-through
 * ([WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE] + [WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE])
 * so the screen / dialog behind it stays fully interactive while a banner is on screen — toasts are
 * non-interactive and auto-dismiss on their own.
 *
 * Every toast in [ToastController.uiState] is rendered, stacked top-to-bottom, so several messages
 * can be visible at once. Each entry animates in (slide + fade), holds for [TOAST_DURATION_MILLIS],
 * then animates out (shrink + fade); only once its exit animation finishes does it call back into
 * [ToastController.dismiss], which keeps neighbours reflowing smoothly as the stack grows and drains.
 */
@Composable
public fun GlobalToastHost(
    controller: ToastController,
    modifier: Modifier = Modifier,
) {
    val toasts by controller.uiState.collectAsStateWithLifecycle()
    if (toasts.isEmpty()) return

    Dialog(
        onDismissRequest = { /* Toasts auto-dismiss; the window is non-interactive. */ },
        properties =
            ModalDefaults.fullScreenDialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
    ) {
        // Let touches and focus fall through to the screen / dialog behind, and disable the platform
        // window animation so only the per-toast Compose transitions drive the motion.
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.apply {
                setWindowAnimations(0)
                addFlags(
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                )
                clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                setDimAmount(0f)
            }
        }

        Box(
            modifier =
                modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 24.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                toasts.forEach { toast ->
                    key(toast.id) {
                        ToastStackItem(
                            toast = toast,
                            onDismiss = { controller.dismiss(toast.id) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * A single auto-dismissing entry in the toast stack. Owns its own [MutableTransitionState] so the
 * enter animation fires on first composition and the exit animation plays in full before
 * [onDismiss] removes it from the controller (which is what makes the stack reflow gracefully).
 */
@Composable
private fun ToastStackItem(
    toast: ToastUiState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Start hidden then immediately target visible so the enter transition animates once.
    val transitionState = remember { MutableTransitionState(false).apply { targetState = true } }

    LaunchedEffect(toast.id) {
        delay(TOAST_DURATION_MILLIS)
        transitionState.targetState = false
        // Wait for the exit animation to fully settle before dropping the toast from the stack.
        snapshotFlow { transitionState.isIdle && !transitionState.currentState }.first { it }
        onDismiss()
    }

    val accentColor: Color
    val icon: ImageVector
    when (toast.style) {
        ToastStyle.SUCCESS -> {
            accentColor = ToastSuccessColor
            icon = Icons.Default.CheckCircle
        }

        ToastStyle.ERROR -> {
            accentColor = ToastErrorColor
            icon = Icons.Default.Warning
        }

        ToastStyle.INFO -> {
            accentColor = ToastInfoColor
            icon = toast.icon ?: Icons.Default.Info
        }
    }

    AnimatedVisibility(
        visibleState = transitionState,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
        modifier = modifier,
    ) {
        ToastSurface(
            message = toast.message,
            icon = icon,
            accentColor = accentColor,
        )
    }
}
