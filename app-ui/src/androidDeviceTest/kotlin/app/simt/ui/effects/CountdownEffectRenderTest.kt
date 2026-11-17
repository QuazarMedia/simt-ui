package app.simt.ui.effects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CountdownEffectRenderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun disablingMotionMakesTheNumberVisibleAndStaticWithoutWaitingForPulse() {
        val motion = mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalUiMotionEnabled provides motion.value, LocalUiHapticsEnabled provides false) {
                    Box(Modifier.size(240.dp).background(Color.Blue).testTag("capture")) {
                        CountdownEffect(Color.Red, startValue = 1, tickMs = 10_000L, initialScale = 4f, scaleDurationMs = 2000, fadeDurationMs = 2000)
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(64)
        compose.runOnIdle { motion.value = false }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        val first = compose.onNodeWithTag("capture").captureToImage().toPixelMap()
        var visiblePixels = 0
        for (y in 0 until first.height) {
            for (x in 0 until first.width) {
                if (first[x, y].red > 0.95f && first[x, y].blue < 0.05f) visiblePixels++
            }
        }
        assertTrue("Number must be opaque immediately", visiblePixels > 100)
        compose.mainClock.advanceTimeBy(512)
        val second = compose.onNodeWithTag("capture").captureToImage().toPixelMap()
        assertEquals(first.width, second.width)
        assertEquals(first.height, second.height)
        for (y in 0 until first.height) {
            for (x in 0 until first.width) {
                assertEquals("Static countdown changed at ($x, $y)", first[x, y], second[x, y])
            }
        }
    }
}
