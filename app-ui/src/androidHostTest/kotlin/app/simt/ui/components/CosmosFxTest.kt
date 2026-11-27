package app.simt.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import app.simt.ui.components.backgrounds.CosmosSpecks
import app.simt.ui.effects.CosmosTier
import app.simt.ui.effects.LocalCosmosTier
import app.simt.ui.effects.LocalUiMotionEnabled
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
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
class CosmosFxTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun activeWavePublishesProgressOnlyWhileActive() {
        var active by mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                CosmosWaveField(
                    accent = Color.Cyan,
                    secondary = Color.Magenta,
                    active = active,
                    icon = Icons.Rounded.Search,
                )
            }
        }

        compose.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertExists()
        compose.runOnIdle { active = false }
        compose.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertDoesNotExist()
    }

    @Test
    fun disabledMotionKeepsActiveWaveProgress() {
        compose.setContent {
            CompositionLocalProvider(LocalUiMotionEnabled provides false) {
                MaterialTheme {
                    CosmosWaveField(
                        accent = Color.Cyan,
                        secondary = Color.Magenta,
                        active = true,
                        icon = Icons.Rounded.Search,
                    )
                }
            }
        }

        compose.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertExists()
    }

    @Test
    fun specksRejectEmptyColors() {
        assertCompositionRejects { CosmosSpecks(colors = emptyList()) }
    }

    @Test
    fun specksAllowZeroCountWithoutColors() {
        compose.setContent {
            MaterialTheme {
                CosmosSpecks(colors = emptyList(), count = 0, modifier = Modifier.testTag("specks"))
            }
        }

        compose.onNodeWithTag("specks").assertExists()
    }

    @Test
    fun specksRejectNegativeCounts() {
        assertCompositionRejects { CosmosSpecks(colors = listOf(Color.Red), count = -1) }
    }

    @Test
    fun successBurstExpandsItsOrbDuringEntrance() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CosmosSuccessBurst(accent = Color.Red, modifier = Modifier.testTag("burst"))
                }
            }
        }

        compose.mainClock.advanceTimeByFrame()
        val initialPixels = centralAccentPixels("burst")
        compose.mainClock.advanceTimeBy(1_000)
        val finalPixels = centralAccentPixels("burst")

        assertTrue("initial=$initialPixels final=$finalPixels", finalPixels > initialPixels + 500)
    }

    @Test
    fun disabledMotionRendersSuccessAtItsFinalStateImmediately() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalUiMotionEnabled provides false) {
                MaterialTheme {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CosmosSuccessBurst(accent = Color.Red, modifier = Modifier.testTag("burst"))
                    }
                }
            }
        }

        compose.mainClock.advanceTimeByFrame()

        val pixels = centralAccentPixels("burst")
        assertTrue("pixels=$pixels", pixels > 500)
    }

    @Test
    fun staticTierRendersErrorAtItsFinalStateImmediately() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalCosmosTier provides CosmosTier.STATIC) {
                MaterialTheme {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CosmosErrorBurst(color = Color.Red, modifier = Modifier.testTag("burst"))
                    }
                }
            }
        }

        compose.mainClock.advanceTimeByFrame()

        val pixels = centralAccentPixels("burst")
        assertTrue("pixels=$pixels", pixels > 500)
    }

    private fun centralAccentPixels(tag: String): Int {
        val image = compose.onNodeWithTag(tag).captureToImage()
        val pixels = image.toPixelMap()
        val centerX = image.width / 2
        val centerY = image.height / 2
        val radius = minOf(image.width, image.height) / 5
        var count = 0
        for (y in centerY - radius..centerY + radius) {
            for (x in centerX - radius..centerX + radius) {
                val color = pixels[x, y]
                if (color.red > 0.95f && color.green < 0.5f && color.blue < 0.5f) {
                    count++
                }
            }
        }
        return count
    }

    private fun assertCompositionRejects(content: @Composable () -> Unit) {
        try {
            compose.setContent { MaterialTheme { content() } }
            compose.waitForIdle()
            fail("Expected invalid Cosmos input to be rejected")
        } catch (_: IllegalArgumentException) {
        }
    }
}
