package app.shared.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TopOverlayHeaderDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun toolsAndCloseHaveNonoverlappingMinimumLayoutAndTouchTargetsAndEdgeTaps() {
        val calls = mutableListOf<String>()
        compose.setContent {
            MaterialTheme {
                TopOverlayHeader(
                    title = "Title",
                    onClose = { calls.add("close") },
                    applyTopInsets = false,
                    tools =
                        listOf(
                            TopOverlayToolToggle(false, Icons.Default.Star, onClick = { calls.add("first:$it") }, contentDescription = "First"),
                            TopOverlayToolToggle(true, Icons.Default.Info, onClick = { calls.add("second:$it") }, contentDescription = "Second"),
                        ),
                )
            }
        }
        val targets = listOf("First", "Second", "Close").map { compose.onNodeWithContentDescription(it) }
        val nodes = targets.map { it.fetchSemanticsNode() }
        val minimum = with(compose.density) { 48.dp.toPx() }
        nodes.forEach { node ->
            assertTrue("Layout width ${node.boundsInRoot.width} < $minimum", node.boundsInRoot.width >= minimum)
            assertTrue("Layout height ${node.boundsInRoot.height} < $minimum", node.boundsInRoot.height >= minimum)
            assertTrue("Touch width ${node.touchBoundsInRoot.width} < $minimum", node.touchBoundsInRoot.width >= minimum)
            assertTrue("Touch height ${node.touchBoundsInRoot.height} < $minimum", node.touchBoundsInRoot.height >= minimum)
        }
        for (i in nodes.indices) {
            for (j in i + 1 until nodes.size) {
                assertTrue("Layout targets overlap", !nodes[i].boundsInRoot.overlaps(nodes[j].boundsInRoot))
                assertTrue("Touch targets overlap", !nodes[i].touchBoundsInRoot.overlaps(nodes[j].touchBoundsInRoot))
            }
        }
        val edge = with(compose.density) { 1.dp.toPx() }
        targets.forEach { target ->
            target.performTouchInput { click(Offset(edge, height / 2f)) }
            target.performTouchInput {
                advanceEventTime(500)
                click(Offset(width - edge, height / 2f))
            }
        }
        compose.runOnIdle {
            assertEquals(listOf("first:false", "first:false", "second:true", "second:true", "close", "close"), calls)
        }
    }

    @Test
    fun disabledLabeledToolRejectsCenterEdgeAndLongPressThenUsesReplacementCallbackWhenEnabled() {
        val original = mutableListOf<Boolean>()
        val replacement = mutableListOf<Boolean>()
        var closes = 0
        val tool =
            mutableStateOf(
                TopOverlayToolToggle(true, Icons.Default.Info, onClick = { original.add(it) }, contentDescription = "Tool", disabled = true),
            )
        compose.setContent {
            MaterialTheme {
                TopOverlayHeader("Title", { closes++ }, applyTopInsets = false, tools = listOf(tool.value))
            }
        }
        val target = compose.onNodeWithContentDescription("Tool")
        target.assertIsNotEnabled()
        val edge = with(compose.density) { 1.dp.toPx() }
        target.performTouchInput { click() }
        target.performTouchInput { click(Offset(edge, height / 2f)) }
        target.performTouchInput { click(Offset(width - edge, height / 2f)) }
        target.performTouchInput { longClick() }
        compose.onNodeWithText("Tool").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(emptyList<Boolean>(), original)
            assertEquals(emptyList<Boolean>(), replacement)
            assertEquals(0, closes)
            tool.value = tool.value.copy(disabled = false, isOn = false, onClick = { replacement.add(it) })
        }
        target.performTouchInput { click(Offset(edge, height / 2f)) }
        compose.runOnIdle {
            assertEquals(emptyList<Boolean>(), original)
            assertEquals(listOf(false), replacement)
            assertEquals(0, closes)
        }
    }
}
