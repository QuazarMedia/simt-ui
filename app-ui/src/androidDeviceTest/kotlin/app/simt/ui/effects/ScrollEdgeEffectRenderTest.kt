package app.simt.ui.effects

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.simt.ui.components.WindowedList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ScrollEdgeEffectRenderTest {
    @get:Rule
    val compose = createComposeRule()

    private val fadeBand = 32.dp

    @Test
    fun verticalAlphaMaskTracksForwardAndBackwardEdges() {
        val state = scene(Orientation.Vertical)
        assertEdges(capture(), Orientation.Vertical, lowFaded = false, highFaded = true)
        scrollTo(state, state.maxValue / 2)
        assertEdges(capture(), Orientation.Vertical, lowFaded = true, highFaded = true)
        scrollTo(state, state.maxValue)
        assertEdges(capture(), Orientation.Vertical, lowFaded = true, highFaded = false)
        scrollTo(state, 0)
        assertEdges(capture(), Orientation.Vertical, lowFaded = false, highFaded = true)
    }

    @Test
    fun disablingFadeKeepsEdgesOpaqueEvenWhenBothCanScroll() {
        val state = scene(Orientation.Vertical, fade = false)
        scrollTo(state, state.maxValue / 2)
        assertEdges(capture(), Orientation.Vertical, lowFaded = false, highFaded = false)
        scrollTo(state, state.maxValue)
        assertEdges(capture(), Orientation.Vertical, lowFaded = false, highFaded = false)
    }

    @Test
    fun horizontalLtrMasksThePhysicalRightThenLeftEdge() {
        val state = scene(Orientation.Horizontal, direction = LayoutDirection.Ltr)
        assertEdges(capture(), Orientation.Horizontal, lowFaded = false, highFaded = true)
        scrollTo(state, state.maxValue / 2)
        assertEdges(capture(), Orientation.Horizontal, lowFaded = true, highFaded = true)
        scrollTo(state, state.maxValue)
        assertEdges(capture(), Orientation.Horizontal, lowFaded = true, highFaded = false)
    }

    @Test
    fun horizontalRtlMasksThePhysicalLeftThenRightEdge() {
        val state = scene(Orientation.Horizontal, direction = LayoutDirection.Rtl)
        assertEdges(capture(), Orientation.Horizontal, lowFaded = true, highFaded = false)
        scrollTo(state, state.maxValue / 2)
        assertEdges(capture(), Orientation.Horizontal, lowFaded = true, highFaded = true)
        scrollTo(state, state.maxValue)
        assertEdges(capture(), Orientation.Horizontal, lowFaded = false, highFaded = true)
    }

    @Test
    fun reversedWindowedListMapsFadesToTheVisualEdges() {
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalUiMotionEnabled provides false) {
                    Box(Modifier.size(160.dp).background(Color.Blue).testTag("capture")) {
                        WindowedList(
                            modifier = Modifier.fillMaxSize(),
                            reverseLayout = true,
                            fadeSize = fadeBand,
                            showArrows = false,
                        ) {
                            items(8) {
                                Box(Modifier.fillMaxWidth().height(80.dp).background(Color.Red))
                            }
                        }
                    }
                }
            }
        }
        assertEdges(capture(), Orientation.Vertical, lowFaded = true, highFaded = false)
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(7)
        assertEdges(capture(), Orientation.Vertical, lowFaded = false, highFaded = true)
    }

    @Test
    fun zeroFadeBandLeavesOverflowingContentOpaque() {
        val state = scene(Orientation.Vertical, zeroBand = true)
        scrollTo(state, state.maxValue / 2)
        assertEdges(capture(), Orientation.Vertical, lowFaded = false, highFaded = false)
    }

    @Test
    fun disablingMotionMakesArrowsStaticAndVisibilityChangesImmediate() {
        val motion = mutableStateOf(true)
        val state = scene(Orientation.Vertical, fade = false, arrows = true, motion = motion)
        compose.runOnIdle { motion.value = false }
        compose.waitForIdle()
        val first = capture()
        assertTrue("A decorative end arrow must be rendered", whitePixels(first, low = false) > 0)
        assertEquals(0, whitePixels(first, low = true))
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(ScrollEdgeEffectDefaults.ArrowAnimationDurationMs.toLong() / 2)
        val later = capture()
        assertPixelsEqual(first, later)
        scrollTo(state, state.maxValue)
        compose.mainClock.advanceTimeByFrame()
        val atEnd = capture()
        assertTrue("Start arrow appears without a timed transition", whitePixels(atEnd, low = true) > 0)
        assertEquals("End arrow disappears without a timed transition", 0, whitePixels(atEnd, low = false))
    }

    private fun scene(
        orientation: Orientation,
        direction: LayoutDirection = LayoutDirection.Ltr,
        fade: Boolean = true,
        zeroBand: Boolean = false,
        arrows: Boolean = false,
        motion: MutableState<Boolean> = mutableStateOf(false),
    ): ScrollState {
        val state = ScrollState(0)
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalUiMotionEnabled provides motion.value,
                    LocalLayoutDirection provides direction,
                ) {
                    Box(Modifier.size(160.dp).background(Color.Blue).testTag("capture")) {
                        ScrollEdgeEffect(
                            state = state,
                            orientation = orientation,
                            modifier = Modifier.fillMaxSize(),
                            fade = fade,
                            fadeSize = if (zeroBand) 0.dp else fadeBand,
                            showArrows = arrows,
                            arrowTint = Color.White,
                        ) {
                            if (orientation == Orientation.Vertical) {
                                Column(Modifier.fillMaxSize().verticalScroll(state)) {
                                    Box(Modifier.size(160.dp, 640.dp).background(Color.Red))
                                }
                            } else {
                                Row(Modifier.fillMaxSize().horizontalScroll(state)) {
                                    Box(Modifier.size(640.dp, 160.dp).background(Color.Red))
                                }
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.runOnIdle { assertTrue("Synthetic content must overflow", state.maxValue > 0) }
        return state
    }

    private fun scrollTo(
        state: ScrollState,
        offset: Int,
    ) {
        compose.runOnIdle { runBlocking { state.scrollTo(offset) } }
        compose.waitForIdle()
    }

    private fun capture(): PixelMap = compose.onNodeWithTag("capture").captureToImage().toPixelMap()

    private fun assertEdges(
        pixels: PixelMap,
        orientation: Orientation,
        lowFaded: Boolean,
        highFaded: Boolean,
    ) {
        val bandPx = with(compose.density) { fadeBand.toPx() }
        val near = (bandPx / 4f).toInt()
        val middle = (bandPx / 2f).toInt()
        val length = if (orientation == Orientation.Vertical) pixels.height else pixels.width

        fun sample(position: Int): Color = if (orientation == Orientation.Vertical) pixels[pixels.width / 2, position] else pixels[position, pixels.height / 2]

        assertBlend("opaque interior", sample(length / 2), 1f)
        for ((low, faded) in listOf(true to lowFaded, false to highFaded)) {
            for (distance in listOf(near, middle)) {
                val position = if (low) distance else length - 1 - distance
                val expectedAlpha = if (faded) (distance + 0.5f) / bandPx else 1f
                assertBlend("$orientation low=$low faded=$faded distance=$distance", sample(position), expectedAlpha)
            }
        }
    }

    private fun assertBlend(
        label: String,
        color: Color,
        foregroundAlpha: Float,
    ) {
        // Red over blue: DstIn must reveal the blue parent, not paint black over red.
        assertEquals("$label red", foregroundAlpha, color.red, 0.06f)
        assertEquals("$label green", 0f, color.green, 0.02f)
        assertEquals("$label blue", 1f - foregroundAlpha, color.blue, 0.06f)
        assertEquals("$label composite alpha", 1f, color.alpha, 0.02f)
    }

    private fun whitePixels(
        pixels: PixelMap,
        low: Boolean,
    ): Int {
        val bandPx = with(compose.density) { fadeBand.toPx() }.toInt()
        val start = if (low) 0 else pixels.height - bandPx
        var count = 0
        for (y in start until start + bandPx) {
            for (x in 0 until pixels.width) {
                val color = pixels[x, y]
                if (color.red > 0.8f && color.green > 0.8f && color.blue > 0.8f) count++
            }
        }
        return count
    }

    private fun assertPixelsEqual(
        first: PixelMap,
        second: PixelMap,
    ) {
        assertEquals(first.width, second.width)
        assertEquals(first.height, second.height)
        for (y in 0 until first.height) {
            for (x in 0 until first.width) {
                assertEquals("Static rendering changed at ($x, $y)", first[x, y], second[x, y])
            }
        }
    }
}
