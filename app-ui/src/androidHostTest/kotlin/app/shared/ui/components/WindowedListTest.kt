package app.shared.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import app.shared.ui.effects.LocalUiMotionEnabled
import app.shared.ui.effects.ScrollEdgeEffectDefaults
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
class WindowedListTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun emptyPlaceholderIsCenteredAndDoesNotComposeRows() {
        var rows = 0
        compose.setContent {
            MaterialTheme {
                WindowedList(
                    items = emptyList<Int>(),
                    key = { it },
                    modifier = Modifier.size(240.dp, 180.dp).testTag("window"),
                    emptyContent = { Text("No synthetic rows", Modifier.testTag("empty")) },
                ) { _, _ -> rows++ }
            }
        }
        compose.onNodeWithTag("empty").assertIsDisplayed()
        val window = compose.onNodeWithTag("window").fetchSemanticsNode().boundsInRoot
        val placeholder = compose.onNodeWithTag("empty").fetchSemanticsNode().boundsInRoot
        assertEquals(window.center.x, placeholder.center.x, 1f)
        assertEquals(window.center.y, placeholder.center.y, 1f)
        compose.runOnIdle { assertEquals(0, rows) }
    }

    @Test
    fun populationReplacesPlaceholderAndSharedStateScrollsToHiddenRows() {
        val items = mutableStateOf(emptyList<Int>())
        lateinit var state: LazyListState
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalUiMotionEnabled provides false) {
                    state = rememberLazyListState()
                    WindowedList(
                        items = items.value,
                        key = { it },
                        state = state,
                        modifier = Modifier.size(240.dp, 180.dp),
                        emptyContent = { Text("No rows", Modifier.testTag("empty")) },
                    ) { index, item ->
                        Text("$index:$item", Modifier.fillMaxWidth().height(60.dp).testTag("row-$item"))
                    }
                }
            }
        }
        compose.onNodeWithTag("empty").assertIsDisplayed()
        compose.runOnIdle { items.value = (0 until 50).toList() }
        compose.onNodeWithTag("empty").assertDoesNotExist()
        compose.onNodeWithTag("row-0").assertIsDisplayed().assertTextEquals("0:0")
        compose.onNodeWithTag("row-49").assertDoesNotExist()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(49)
        compose.onNodeWithTag("row-49").assertIsDisplayed().assertTextEquals("49:49")
        compose.runOnIdle {
            assertTrue(state.firstVisibleItemIndex > 0)
            assertTrue(state.canScrollBackward)
            assertEquals(50, state.layoutInfo.totalItemsCount)
        }
    }

    @Test
    fun stableKeysKeepRememberedStateWithTheItemWhenReordered() {
        val items = mutableStateOf(listOf("a", "b", "c"))
        compose.setContent {
            MaterialTheme {
                WindowedList(
                    items = items.value,
                    key = { it },
                    contentType = { "synthetic-row" },
                    modifier = Modifier.size(240.dp, 240.dp),
                    showArrows = false,
                ) { index, item ->
                    val clicks = remember { mutableStateOf(0) }
                    Text(
                        "$item:$index:${clicks.value}",
                        Modifier.fillMaxWidth().height(60.dp).testTag(item).clickable { clicks.value++ },
                    )
                }
            }
        }
        compose.onNodeWithTag("b").performTouchInput { click() }
        compose.onNodeWithTag("b").assertTextEquals("b:1:1")
        compose.runOnIdle { items.value = listOf("b", "c", "a") }
        compose.onNodeWithTag("b").assertTextEquals("b:0:1")
        compose.onNodeWithTag("c").assertTextEquals("c:1:0")
        compose.onNodeWithTag("a").assertTextEquals("a:2:0")
        val b = compose.onNodeWithTag("b").fetchSemanticsNode().boundsInRoot
        val a = compose.onNodeWithTag("a").fetchSemanticsNode().boundsInRoot
        assertTrue(b.top < a.top)
    }

    @Test
    fun untypedOverloadKeepsReverseLayoutAndScrollState() {
        lateinit var state: LazyListState
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalUiMotionEnabled provides false) {
                    state = rememberLazyListState()
                    WindowedList(
                        state = state,
                        modifier = Modifier.size(240.dp, 180.dp),
                        reverseLayout = true,
                    ) {
                        items(30) { item ->
                            Text("Row $item", Modifier.fillMaxWidth().height(60.dp).testTag("row-$item"))
                        }
                    }
                }
            }
        }
        val zero = compose.onNodeWithTag("row-0").fetchSemanticsNode().boundsInRoot
        val one = compose.onNodeWithTag("row-1").fetchSemanticsNode().boundsInRoot
        assertTrue("Item zero must be at the visual bottom", zero.top > one.top)
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(29)
        compose.onNodeWithTag("row-29").assertIsDisplayed()
        compose.runOnIdle {
            assertTrue(state.layoutInfo.reverseLayout)
            assertTrue(state.canScrollBackward)
        }
    }

    @Test
    fun decorativeArrowBandsDoNotInterceptRealTapsAtEitherEdge() {
        val clicks = mutableListOf<Int>()
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalUiMotionEnabled provides false) {
                    WindowedList(modifier = Modifier.size(240.dp, 96.dp).testTag("window")) {
                        items(3) { item ->
                            Box(Modifier.fillMaxWidth().height(96.dp).testTag("row-$item").clickable { clicks.add(item) })
                        }
                    }
                }
            }
        }
        val bandHalf = with(compose.density) { ScrollEdgeEffectDefaults.VerticalFadeSize.toPx() / 2f }
        compose.onAllNodes(hasClickAction()).assertCountEquals(1)
        compose.onNodeWithTag("window").performTouchInput {
            click(Offset(width / 2f, height - bandHalf))
        }
        compose.runOnIdle { assertEquals(listOf(0), clicks) }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(2)
        compose.onNodeWithTag("row-2").assertIsDisplayed()
        compose.onAllNodes(hasClickAction()).assertCountEquals(1)
        compose.onNodeWithTag("window").performTouchInput {
            click(Offset(width / 2f, bandHalf))
        }
        compose.runOnIdle { assertEquals(listOf(0, 2), clicks) }
    }
}
