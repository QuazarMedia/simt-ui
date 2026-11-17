package app.shared.ui.effects

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/** Effective caller-owned reduced-motion/visibility policy, independent of brand theme. */
val LocalUiMotionEnabled: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { true }
