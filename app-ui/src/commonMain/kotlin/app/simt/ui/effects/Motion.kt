package app.simt.ui.effects

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Central Cosmos motion specification.
 *
 * Use these tokens instead of ad-hoc `tween(...)`/`spring(...)` values so motion feels
 * consistent across the app and can be tuned in one place.
 */
public object SimtMotion {
    // Durations (ms)
    public const val FAST: Int = 150
    public const val MEDIUM: Int = 300

    /** Emphasized decelerate — entrances, expansions. */
    public val EnterEasing: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Standard accelerate — exits, dismissals. */
    public val ExitEasing: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Standard in-place transform easing. */
    public val TransformEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    public fun <T> enterSpec(durationMs: Int = MEDIUM): FiniteAnimationSpec<T> = tween<T>(durationMs, easing = EnterEasing)

    public fun <T> exitSpec(durationMs: Int = FAST): FiniteAnimationSpec<T> = tween<T>(durationMs, easing = ExitEasing)
}

/**
 * Rendering tier for cosmetic animation, derived from the device performance level.
 *
 * - [FULL] — parallax + nebula drift + twinkle + ripples + staggered entrances.
 * - [REDUCED] — parallax and entrances only; no infinite animations.
 * - [STATIC] — cached static rendering only; zero animation cost.
 */
public enum class CosmosTier {
    FULL,
    REDUCED,
    STATIC,
    ;

    public val allowsInfiniteAnimations: Boolean get() = this == FULL
    public val allowsParallax: Boolean get() = this != STATIC
    public val allowsEntranceAnimations: Boolean get() = this != STATIC
}

/**
 * Ambient rendering tier for the current composition.
 *
 * Defaults to [CosmosTier.FULL]; app roots should provide it from their device performance
 * level so shared UI can degrade gracefully on low-end hardware.
 */
public val LocalCosmosTier: ProvidableCompositionLocal<CosmosTier> =
    staticCompositionLocalOf { CosmosTier.FULL }
