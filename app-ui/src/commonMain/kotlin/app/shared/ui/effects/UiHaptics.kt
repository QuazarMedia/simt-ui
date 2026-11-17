package app.shared.ui.effects

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/** Caller-owned haptic policy, independent of motion. */
val LocalUiHapticsEnabled: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { true }
