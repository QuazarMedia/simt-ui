package app.simt.ui.transitions

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import app.simt.ui.effects.SimtMotion

public fun appScreenTransition(
    enterDurationMs: Int = SimtMotion.MEDIUM,
    exitDurationMs: Int = SimtMotion.FAST,
    initialScale: Float = 0.94f,
): ContentTransform =
    (
        fadeIn(animationSpec = tween(enterDurationMs, easing = SimtMotion.EnterEasing)) +
            scaleIn(
                initialScale = initialScale,
                animationSpec = tween(enterDurationMs, easing = SimtMotion.EnterEasing),
            )
    ) togetherWith fadeOut(animationSpec = tween(exitDurationMs, easing = SimtMotion.ExitEasing))

public fun appScreenEnterTransition(
    durationMs: Int = SimtMotion.MEDIUM,
    initialScale: Float = 0.94f,
): EnterTransition =
    fadeIn(animationSpec = tween(durationMs, easing = SimtMotion.EnterEasing)) +
        scaleIn(
            initialScale = initialScale,
            animationSpec = tween(durationMs, easing = SimtMotion.EnterEasing),
        )

public fun appScreenExitTransition(durationMs: Int = SimtMotion.FAST): ExitTransition = fadeOut(animationSpec = tween(durationMs, easing = SimtMotion.ExitEasing))
