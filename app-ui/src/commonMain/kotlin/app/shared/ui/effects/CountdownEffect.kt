package app.shared.ui.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.shared.ui.internal.platform.countdownStartDispatcher
import app.shared.ui.internal.platform.rememberCountdownHaptics
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
public fun CountdownEffect(
    color: Color,
    modifier: Modifier = Modifier,
    startValue: Int = 3,
    tickMs: Long = 1000L,
    onStart: (suspend () -> Unit)? = null,
    onFinished: () -> Unit = {},
    subtitle: String? = null,
    subtitleColor: Color = Color.White.copy(alpha = 0.7f),
    fontSize: TextUnit = 120.sp,
    initialScale: Float = 2.0f,
    scaleDurationMs: Int = 500,
    fadeDurationMs: Int = 200,
) {
    var currentValue by remember { mutableIntStateOf(startValue) }
    val haptics by rememberUpdatedState(rememberCountdownHaptics())
    val hapticsEnabled by rememberUpdatedState(LocalUiHapticsEnabled.current)
    val finished by rememberUpdatedState(onFinished)
    val motionEnabled = LocalUiMotionEnabled.current

    LaunchedEffect(Unit) {
        if (onStart != null) {
            launch(countdownStartDispatcher) { onStart() }
        }
        for (count in startValue downTo 1) {
            currentValue = count
            if (hapticsEnabled) {
                if (count > 1) haptics.tick() else haptics.finalTick()
            }
            delay(tickMs)
        }
        finished()
    }

    val scale = remember(currentValue) { Animatable(initialScale) }
    val alpha = remember(currentValue) { Animatable(0f) }
    LaunchedEffect(currentValue, motionEnabled) {
        if (motionEnabled) {
            coroutineScope {
                launch { alpha.animateTo(1f, tween(fadeDurationMs, easing = FastOutSlowInEasing)) }
                launch { scale.animateTo(1f, tween(scaleDurationMs, easing = FastOutSlowInEasing)) }
            }
        }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = currentValue.toString(),
            modifier =
                Modifier.graphicsLayer {
                    scaleX = if (motionEnabled) scale.value else 1f
                    scaleY = if (motionEnabled) scale.value else 1f
                    this.alpha = if (motionEnabled) alpha.value else 1f
                },
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = subtitle, style = MaterialTheme.typography.titleLarge, color = subtitleColor)
        }
    }
}
