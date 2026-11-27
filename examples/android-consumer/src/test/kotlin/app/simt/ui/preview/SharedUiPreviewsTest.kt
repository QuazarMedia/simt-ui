package app.simt.ui.preview

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@OptIn(ExperimentalTestApi::class)
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "en-w420dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SharedUiPreviewsTest(private val sample: Sample) {
    enum class Sample {
        FIELDS,
        LOCATION,
        TYPED_LIST,
        EMPTY_LIST,
        DSL_LIST,
        SCROLL_EDGES,
        TOOLTIP,
        HEADER,
        COUNTDOWN,
        INSETS,
        ;

        @Composable
        fun Render() {
            when (this) {
                FIELDS -> ClearableTextFieldPreview()
                LOCATION -> CityCountryFieldsPreview()
                TYPED_LIST -> WindowedListPreview()
                EMPTY_LIST -> WindowedListEmptyPreview()
                DSL_LIST -> WindowedListDslPreview()
                SCROLL_EDGES -> ScrollEdgeEffectPreview()
                TOOLTIP -> LongPressTooltipEffectPreview()
                HEADER -> TopOverlayHeaderPreview()
                COUNTDOWN -> CountdownEffectPreview()
                INSETS -> FullscreenInsetsPreview()
            }
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun samples(): List<Array<Sample>> = Sample.entries.map { arrayOf(it) }
    }

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun actualPreviewComposesWithBoundedLayoutAndRepresentativeSemantics() {
        compose.setContent { sample.Render() }
        compose.waitForIdle()
        when (sample) {
            Sample.FIELDS -> {
                compose.onNodeWithTag("empty-field").assertIsDisplayed().assert(hasText(""))
                compose.onNodeWithTag("populated-field").assert(hasText("Synthetic value"))
                compose.onNodeWithTag("error-field").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
                compose.onNodeWithText("Sample validation error").assertIsDisplayed()
                compose.onNodeWithText("Synthetic supporting text").assertIsDisplayed()
                compose.onNodeWithTag("readonly-field").assert(hasText("Read-only sample")).assert(!hasSetTextAction())
                compose.onNodeWithTag("populated-field").performTextReplacement("Edited sample")
                compose.onNodeWithTag("populated-field").assert(hasText("Edited sample"))
            }
            Sample.LOCATION -> {
                compose.onNodeWithText("City name").assertIsDisplayed().performTextReplacement("New Sampleville")
                compose.onNodeWithText("New Sampleville").assertExists()
                compose.onNodeWithText("Country Code (2 letters)").performTextReplacement("es")
                compose.onNodeWithText("ES").assertExists()
            }
            Sample.TYPED_LIST -> {
                compose.onNodeWithTag("typed-list").assertHeightIsEqualTo(240.dp)
                compose.onNodeWithText("Sample row 10").assertIsDisplayed()
                compose.onNodeWithText("Sample row 0").assertDoesNotExist()
                compose.onNode(hasScrollToIndexAction()).performScrollToIndex(29)
                compose.onNodeWithText("Sample row 29").assertIsDisplayed()
            }
            Sample.EMPTY_LIST -> compose.onNodeWithText("No synthetic rows").assertIsDisplayed()
            Sample.DSL_LIST -> {
                compose.onNodeWithTag("dsl-list").assertHeightIsEqualTo(240.dp)
                compose.onNodeWithText("Synthetic section").assertIsDisplayed()
                compose.onNode(hasScrollToIndexAction()).performScrollToIndex(20)
                compose.onNodeWithText("DSL row 19").assertIsDisplayed()
            }
            Sample.SCROLL_EDGES -> {
                val horizontal = compose.onNodeWithTag("horizontal-scroll").assertHeightIsEqualTo(72.dp)
                val vertical = compose.onNodeWithTag("vertical-scroll").assertHeightIsEqualTo(180.dp)
                val horizontalBefore = horizontal.fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange].value()
                val verticalBefore = vertical.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
                assertMidrange(horizontal.fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange])
                assertMidrange(vertical.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange])
                horizontal.performTouchInput { swipeLeft() }
                vertical.performTouchInput { swipeUp() }
                assertTrue(horizontal.fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange].value() > horizontalBefore)
                assertTrue(vertical.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value() > verticalBefore)
            }
            Sample.TOOLTIP -> {
                compose.onNodeWithText("Long press for sample help").assertIsDisplayed()
                compose.onNodeWithTag("tooltip-anchor").performClick()
                compose.onNodeWithText("Synthetic tooltip help").assertDoesNotExist()
            }
            Sample.HEADER -> {
                compose.onNodeWithText("Survey").assertIsDisplayed()
                compose.onNodeWithText("Synthetic session").assertIsDisplayed()
                compose.onNodeWithText("Synthetic header card").assertIsDisplayed()
                compose.onNodeWithContentDescription("Selected tool").assertIsSelected().performClick().assertIsNotSelected()
                compose.onNodeWithContentDescription("Unselected tool").assertIsNotSelected().performClick().assertIsSelected()
                compose.onNodeWithContentDescription("Disabled tool").assertIsNotEnabled().assertIsNotSelected()
            }
            Sample.COUNTDOWN -> {
                compose.onNodeWithText("1").assertIsDisplayed()
                compose.onNodeWithText("Synthetic countdown").assertIsDisplayed()
                compose.mainClock.advanceTimeBy(2000)
                compose.onNodeWithText("1").assertIsDisplayed()
            }
            Sample.INSETS -> {
                compose.onNodeWithText("Top inset sample").assertIsDisplayed()
                compose.onNodeWithText("Bottom inset sample").assertIsDisplayed()
            }
        }
    }

    @Test
    fun publishedPreviewProducesNativeGraphicsCapture() {
        compose.setContent { sample.Render() }
        compose.waitForIdle()
        val image = compose.onNodeWithTag("preview-surface").captureToImage()
        assertTrue("Captured preview has width", image.width > 0)
        assertTrue("Captured preview has height", image.height > 0)

        val output = System.getProperty("simt.previewOutput") ?: return
        val directory = File(output)
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create preview output directory" }
        val name = sample.name.lowercase().replace('_', '-')
        val target = File(directory, "$name.png")
        target.outputStream().use { stream ->
            check(image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)) { "PNG encoding failed" }
        }
        assertTrue("Published capture is not empty", target.length() > 0)
    }

    private fun assertMidrange(range: ScrollAxisRange) {
        assertTrue("Attached scroll state has real overflow", range.maxValue() > 0f)
        assertTrue("Scroll state starts between both edges", range.value() > 0f && range.value() < range.maxValue())
    }
}
