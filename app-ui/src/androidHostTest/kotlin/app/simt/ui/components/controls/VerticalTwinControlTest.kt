package app.simt.ui.components.controls

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
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
class VerticalTwinControlTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun halvesExposeIndependentButtonSemanticsAndMinimumTargets() {
        var topClicks = 0
        var bottomClicks = 0
        var minimumTarget = 0f
        compose.setContent {
            MaterialTheme {
                minimumTarget = with(LocalDensity.current) { 48.dp.toPx() }
                VerticalTwinControl(
                    topIcon = Icons.Default.Add,
                    bottomIcon = Icons.Default.Remove,
                    topContentDescription = "Zoom in",
                    bottomContentDescription = "Zoom out",
                    onTopClick = { topClicks++ },
                    onBottomClick = { bottomClicks++ },
                )
            }
        }

        val top = compose.onNodeWithContentDescription("Zoom in")
        val bottom = compose.onNodeWithContentDescription("Zoom out")
        val buttonRole = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
        top.assertIsEnabled().assert(buttonRole)
        bottom.assertIsEnabled().assert(buttonRole)
        val bounds = listOf(top, bottom).map { it.fetchSemanticsNode().boundsInRoot }
        bounds.forEach {
            assertTrue("minimum target width: $it", it.width >= minimumTarget)
            assertTrue("minimum target height: $it", it.height >= minimumTarget)
        }
        assertTrue("twin targets must not overlap", bounds[0].bottom <= bounds[1].top)

        top.performClick()
        bottom.performClick()
        compose.runOnIdle {
            assertEquals(1, topClicks)
            assertEquals(1, bottomClicks)
        }
    }

    @Test
    fun enabledStateRecompositionSuppressesOnlyTheDisabledHalf() {
        var topClicks = 0
        var bottomClicks = 0
        var topEnabled by mutableStateOf(false)
        var bottomEnabled by mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                VerticalTwinControl(
                    topIcon = Icons.Default.Add,
                    bottomIcon = Icons.Default.Remove,
                    topContentDescription = "Zoom in",
                    bottomContentDescription = "Zoom out",
                    onTopClick = { topClicks++ },
                    onBottomClick = { bottomClicks++ },
                    topEnabled = topEnabled,
                    bottomEnabled = bottomEnabled,
                    modifier = Modifier,
                )
            }
        }

        val top = compose.onNodeWithContentDescription("Zoom in")
        val bottom = compose.onNodeWithContentDescription("Zoom out")
        top.assertIsNotEnabled()
        bottom.assertIsEnabled()
        top.performClick()
        bottom.performClick()
        compose.runOnIdle {
            assertEquals(0, topClicks)
            assertEquals(1, bottomClicks)
            topEnabled = true
            bottomEnabled = false
        }

        top.assertIsEnabled()
        bottom.assertIsNotEnabled()
        top.performClick()
        bottom.performClick()
        compose.runOnIdle {
            assertEquals(1, topClicks)
            assertEquals(1, bottomClicks)
        }
    }
}
