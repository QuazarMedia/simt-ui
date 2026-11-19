package app.simt.ui.internal.platform

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineDispatcher

internal expect val countdownStartDispatcher: CoroutineDispatcher

@Composable
internal expect fun headerWidthFraction(): Float

@Composable
internal expect fun fullScreenTopWindowInsets(): WindowInsets

@Composable
internal expect fun fullScreenBottomWindowInsets(): WindowInsets

internal interface CountdownHaptics {
    fun tick()

    fun finalTick()
}

@Composable
internal expect fun rememberCountdownHaptics(): CountdownHaptics

/** Parent of the current Compose `View` on Android; null on other platforms. */
@Composable
internal expect fun currentViewParent(): Any?

/** Suppresses platform dialog window animations on Android; no-op on other platforms. */
internal expect fun suppressDialogWindowAnimations(parent: Any?)
