package app.shared.ui.internal.platform

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle.UIImpactFeedbackStyleLight
import platform.UIKit.UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium

internal actual val countdownStartDispatcher: CoroutineDispatcher get() = Dispatchers.IO

@Composable
internal actual fun headerWidthFraction(): Float = 1f

@Composable
internal actual fun fullScreenTopWindowInsets(): WindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top)

@Composable
internal actual fun fullScreenBottomWindowInsets(): WindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Bottom)

@Composable
internal actual fun rememberCountdownHaptics(): CountdownHaptics =
    remember {
        val light = UIImpactFeedbackGenerator(style = UIImpactFeedbackStyleLight)
        val medium = UIImpactFeedbackGenerator(style = UIImpactFeedbackStyleMedium)
        object : CountdownHaptics {
            override fun tick() {
                light.prepare()
                light.impactOccurred()
            }

            override fun finalTick() {
                medium.prepare()
                medium.impactOccurred()
            }
        }
    }
