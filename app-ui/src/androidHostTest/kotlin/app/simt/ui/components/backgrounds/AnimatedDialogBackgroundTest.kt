package app.simt.ui.components.backgrounds

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import app.simt.ui.components.permissions.AnimatedDialogBackgroundStyle
import org.junit.Assert.assertArrayEquals
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
class AnimatedDialogBackgroundTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun networkGridUsesSecondaryAccentAndNonNetworkStylesUsePrimary() {
        val primary = Color(0xFF00FF00)
        val secondary = Color(0xFFFF0000)

        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                Box(modifier = Modifier.size(480.dp, 240.dp)) {
                    Row {
                        AnimatedDialogBackground(
                            style = AnimatedDialogBackgroundStyle.NetworkGrid,
                            modifier = Modifier.size(240.dp).testTag("network"),
                            primaryColor = primary,
                            secondaryColor = secondary,
                            frozen = true,
                        )
                        AnimatedDialogBackground(
                            style = AnimatedDialogBackgroundStyle.RadarSweep,
                            modifier = Modifier.size(240.dp).testTag("radar"),
                            primaryColor = primary,
                            secondaryColor = secondary,
                            frozen = true,
                        )
                    }
                }
            }
        }
        compose.mainClock.advanceTimeByFrame()
        val network = capture("network")
        val radar = capture("radar")

        assertTrue("network red=${network.redPixels()}", network.redPixels() > 20)
        assertEquals("radar red=${radar.redPixels()}", 0, radar.redPixels())
        assertTrue("radar green=${radar.greenPixels()}", radar.greenPixels() > 20)
        assertTrue(network.argb().contentEquals(radar.argb()).not())
    }

    @Test
    fun frozenGateHeaderRendersAStaticSkyInItsParentBounds() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                AnimatedDialogBackground(
                    style = AnimatedDialogBackgroundStyle.Geometric,
                    modifier = Modifier.size(320.dp, 180.dp).testTag("gate"),
                    frozen = true,
                )
            }
        }

        compose.mainClock.advanceTimeByFrame()
        val first = capture("gate")
        assertEquals(320, first.width)
        assertEquals(180, first.height)
        compose.mainClock.advanceTimeBy(512)
        val second = capture("gate")

        assertArrayEquals(first.argb(), second.argb())
    }

    @Test
    fun gateHeaderBackgroundFillsItsParentAndForwardsTheFrozenState() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                Box(modifier = Modifier.size(320.dp, 180.dp).testTag("host")) {
                    gateHeaderBackground(accentColor = Color(0xFFFFC766), frozen = true)
                }
            }
        }

        compose.mainClock.advanceTimeByFrame()
        val first = capture("host")
        assertEquals(320, first.width)
        assertEquals(180, first.height)
        compose.mainClock.advanceTimeBy(512)
        val second = capture("host")

        assertArrayEquals(first.argb(), second.argb())
    }

    private fun capture(tag: String) = compose.onNodeWithTag(tag).captureToImage().toPixelMap()

    private fun PixelMap.argb(): IntArray =
        IntArray(width * height) { index ->
            this[index % width, index / width].toArgb()
        }

    private fun PixelMap.redPixels(): Int =
        (0 until width).sumOf { x ->
            (0 until height).count { y ->
                val color = this[x, y]
                color.red > 0.7f && color.green < 0.3f && color.blue < 0.3f
            }
        }

    private fun PixelMap.greenPixels(): Int =
        (0 until width).sumOf { x ->
            (0 until height).count { y ->
                val color = this[x, y]
                color.green > 0.7f && color.red < 0.3f && color.blue < 0.3f
            }
        }
}
