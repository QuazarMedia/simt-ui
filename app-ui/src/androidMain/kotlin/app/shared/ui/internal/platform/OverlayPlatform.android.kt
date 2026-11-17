package app.shared.ui.internal.platform

import android.content.res.Configuration
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val countdownStartDispatcher: CoroutineDispatcher get() = Dispatchers.IO

@Composable
internal actual fun headerWidthFraction(): Float = if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) 0.5f else 1f

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal actual fun fullScreenTopWindowInsets(): WindowInsets =
    WindowInsets.statusBarsIgnoringVisibility.only(WindowInsetsSides.Top)
        .union(WindowInsets.displayCutout.only(WindowInsetsSides.Top))

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal actual fun fullScreenBottomWindowInsets(): WindowInsets =
    WindowInsets.systemBarsIgnoringVisibility.only(WindowInsetsSides.Bottom)
        .union(WindowInsets.displayCutout.only(WindowInsetsSides.Bottom))

@Composable
internal actual fun rememberCountdownHaptics(): CountdownHaptics {
    val view = LocalView.current
    return remember(view) {
        object : CountdownHaptics {
            override fun tick() {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }

            override fun finalTick() {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }
    }
}
