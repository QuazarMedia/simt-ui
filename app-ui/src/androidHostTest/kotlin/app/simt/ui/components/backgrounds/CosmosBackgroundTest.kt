package app.simt.ui.components.backgrounds

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import app.simt.ui.effects.CosmosTier
import app.simt.ui.effects.LocalCosmosTier
import app.simt.ui.effects.LocalUiMotionEnabled
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CosmosBackgroundTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun fullTierAnimatesTheSky() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                CosmosBackground(modifier = Modifier.size(240.dp).testTag("cosmos"), variant = CosmosVariant.Home)
            }
        }

        compose.mainClock.advanceTimeByFrame()
        val first = capture("cosmos")
        compose.mainClock.advanceTimeBy(1_000)
        val second = capture("cosmos")

        val changed = changedPixels(first, second)
        assertTrue("changed=$changed", changed > 100)
    }

    @Test
    fun disabledMotionRendersStaticAmbientSkyWithHeadingParallax() {
        val rotation = mutableStateOf(0f)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalUiMotionEnabled provides false) {
                MaterialTheme {
                    CosmosBackground(
                        modifier = Modifier.size(240.dp).testTag("cosmos"),
                        variant = CosmosVariant.Home,
                        parallaxRotationDegrees = { rotation.value },
                    )
                }
            }
        }

        compose.mainClock.advanceTimeByFrame()
        val first = capture("cosmos")
        compose.mainClock.advanceTimeBy(512)
        val second = capture("cosmos")

        assertStatic(first, second)

        compose.runOnIdle { rotation.value = 35f }
        compose.mainClock.advanceTimeByFrame()
        val parallaxed = capture("cosmos")
        val changed = changedPixels(second, parallaxed)
        assertTrue("changed=$changed", changed > 100)
    }

    @Test
    fun staticTierIgnoresAmbientMotionAndHeadingParallax() {
        val rotation = mutableStateOf(0f)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalCosmosTier provides CosmosTier.STATIC) {
                MaterialTheme {
                    CosmosBackground(
                        modifier = Modifier.size(240.dp).testTag("cosmos"),
                        variant = CosmosVariant.Home,
                        parallaxRotationDegrees = { rotation.value },
                    )
                }
            }
        }

        compose.mainClock.advanceTimeByFrame()
        val first = capture("cosmos")
        compose.runOnIdle { rotation.value = 35f }
        compose.mainClock.advanceTimeByFrame()
        val second = capture("cosmos")

        assertStatic(first, second)
    }

    @Test
    fun frozenFullTierRendersAStaticSky() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                CosmosBackground(modifier = Modifier.size(240.dp).testTag("cosmos"), variant = CosmosVariant.Home, frozen = true)
            }
        }

        compose.mainClock.advanceTimeByFrame()
        val first = capture("cosmos")
        compose.mainClock.advanceTimeBy(512)
        val second = capture("cosmos")

        assertStatic(first, second)
    }

    private fun capture(tag: String) = compose.onNodeWithTag(tag).captureToImage().toPixelMap()

    private fun assertStatic(
        first: PixelMap,
        second: PixelMap,
    ) {
        assertEquals(first.width, second.width)
        assertEquals(first.height, second.height)
        for (y in 0 until first.height) {
            for (x in 0 until first.width) {
                assertEquals("Static cosmos changed at ($x, $y)", first[x, y], second[x, y])
            }
        }
    }

    private fun changedPixels(
        first: PixelMap,
        second: PixelMap,
    ): Int {
        assertEquals(first.width, second.width)
        assertEquals(first.height, second.height)
        var changed = 0
        for (y in 0 until first.height) {
            for (x in 0 until first.width) {
                if (first[x, y] != second[x, y]) changed++
            }
        }
        return changed
    }
}
