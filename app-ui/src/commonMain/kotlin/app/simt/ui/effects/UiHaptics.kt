package app.simt.ui.effects

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/** Caller-owned haptic policy, independent of motion. */
public val LocalUiHapticsEnabled: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { true }
