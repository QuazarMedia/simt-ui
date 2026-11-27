package app.simt.ui.components.controls

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
class MapControlPanelTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun builtInAndAdditionalActionsKeepTheirOrderCallbacksAndMinimumTargets() {
        var recenterClicks = 0
        var zoomInClicks = 0
        var zoomOutClicks = 0
        var filterClicks = 0
        var minimumTarget = 0f
        compose.setContent {
            MaterialTheme {
                minimumTarget = with(LocalDensity.current) { 48.dp.toPx() }
                MapControlPanel(
                    onRecenter = { recenterClicks++ },
                    onZoomIn = { zoomInClicks++ },
                    onZoomOut = { zoomOutClicks++ },
                    recenterContentDescription = "Recenter",
                    zoomInContentDescription = "Zoom in",
                    zoomOutContentDescription = "Zoom out",
                    zoomInfo = { Text("50%") },
                    additionalContent = {
                        MapControlButton(
                            icon = Icons.Default.FilterList,
                            onClick = { filterClicks++ },
                            contentDescription = "Filters",
                        )
                    },
                )
            }
        }

        val recenter = compose.onNodeWithContentDescription("Recenter")
        val zoomIn = compose.onNodeWithContentDescription("Zoom in")
        val zoomOut = compose.onNodeWithContentDescription("Zoom out")
        val filters = compose.onNodeWithContentDescription("Filters")
        val label = compose.onNodeWithText("50%")
        val buttonRole = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
        listOf(recenter, zoomIn, zoomOut, filters).forEach {
            it.assertIsEnabled().assert(buttonRole)
        }
        val bounds = listOf(recenter, zoomIn, zoomOut, filters).map { it.fetchSemanticsNode().boundsInRoot }
        bounds.forEach {
            assertTrue("minimum target width: $it", it.width >= minimumTarget)
            assertTrue("minimum target height: $it", it.height >= minimumTarget)
        }
        bounds.zipWithNext().forEach { (upper, lower) ->
            assertTrue("panel targets must not overlap", upper.bottom <= lower.top)
        }
        val labelBounds = label.fetchSemanticsNode().boundsInRoot
        assertTrue("zoom label must follow zoom in", bounds[1].bottom <= labelBounds.top)
        assertTrue("zoom out must follow zoom label", labelBounds.bottom <= bounds[2].top)

        recenter.performClick()
        zoomIn.performClick()
        zoomOut.performClick()
        filters.performClick()
        compose.runOnIdle {
            assertEquals(1, recenterClicks)
            assertEquals(1, zoomInClicks)
            assertEquals(1, zoomOutClicks)
            assertEquals(1, filterClicks)
        }
    }

    @Test
    fun disabledAdditionalActionSuppressesTapAndTooltipUntilEnabled() {
        var clicks = 0
        var enabled by mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                MapControlButton(
                    icon = Icons.Default.FilterList,
                    onClick = { clicks++ },
                    contentDescription = "Filters",
                    enabled = enabled,
                )
            }
        }

        val filters = compose.onNodeWithContentDescription("Filters")
        filters.assertIsNotEnabled().assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button),
        )
        filters.performClick()
        filters.performTouchInput { longClick() }
        compose.onNodeWithText("Filters").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(0, clicks)
            enabled = true
        }

        filters.assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun zoomLabelRemainsReadableAtLargeFontScale() {
        compose.setContent {
            MaterialTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density = density.density, fontScale = 2f),
                ) {
                    MapControlPanel(
                        onRecenter = {},
                        onZoomIn = {},
                        onZoomOut = {},
                        recenterContentDescription = "Recenter",
                        zoomInContentDescription = "Zoom in",
                        zoomOutContentDescription = "Zoom out",
                        zoomInfo = { MapControlLabel(text = "125%") },
                    )
                }
            }
        }

        val label = compose.onNodeWithText("125%").assertIsDisplayed()
        val zoomIn = compose.onNodeWithContentDescription("Zoom in").fetchSemanticsNode().boundsInRoot
        val zoomOut = compose.onNodeWithContentDescription("Zoom out").fetchSemanticsNode().boundsInRoot
        val labelBounds = label.fetchSemanticsNode().boundsInRoot
        assertTrue("zoom label must follow zoom in", zoomIn.bottom <= labelBounds.top)
        assertTrue("zoom out must follow zoom label", labelBounds.bottom <= zoomOut.top)
    }
}
