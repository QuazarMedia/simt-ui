package app.simt.ui.transitions

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import app.simt.ui.effects.LocalCosmosTier
import app.simt.ui.effects.LocalUiMotionEnabled
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

public fun Modifier.staggeredEntrance(
    index: Int = 0,
    delayMs: Long? = null,
    durationMs: Int = 400,
    offsetDp: Float = 40f,
): Modifier =
    composed {
        val motionEnabled = LocalUiMotionEnabled.current && LocalCosmosTier.current.allowsEntranceAnimations
        val alpha = remember { Animatable(if (motionEnabled) 0f else 1f) }
        val translateY = remember { Animatable(if (motionEnabled) offsetDp else 0f) }

        LaunchedEffect(motionEnabled) {
            if (!motionEnabled) {
                alpha.snapTo(1f)
                translateY.snapTo(0f)
            } else {
                delay(delayMs ?: (index * 100L))
                coroutineScope {
                    launch { alpha.animateTo(1f, tween(durationMs, easing = FastOutSlowInEasing)) }
                    launch { translateY.animateTo(0f, tween(durationMs, easing = FastOutSlowInEasing)) }
                }
            }
        }

        offset { IntOffset(0, translateY.value.roundToInt()) }.graphicsLayer {
            this.alpha = alpha.value
        }
    }
