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
