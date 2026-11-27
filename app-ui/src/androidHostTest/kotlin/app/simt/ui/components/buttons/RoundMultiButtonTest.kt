package app.simt.ui.components.buttons

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.simt.ui.effects.LocalUiMotionEnabled
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoundMultiButtonTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun roundMultiButtonExpandsInvokesItemOnceAndCollapses() {
        var calls = 0
        compose.setContent {
            MaterialTheme {
                RoundMultiButton(
                    icon = Icons.Default.Add,
                    contentDescription = "Add image",
                    items =
                        listOf(
                            RoundMultiButtonItem(Icons.Default.PhotoCamera, { calls++ }, "Camera"),
                            RoundMultiButtonItem(Icons.Default.PhotoLibrary, {}, "Gallery"),
                        ),
                )
            }
        }

        compose.onNodeWithContentDescription("Camera").assertDoesNotExist()
        compose.onNodeWithContentDescription("Add image").performClick()
        compose.onNodeWithContentDescription("Camera").assertIsDisplayed()
        compose.onNodeWithContentDescription("Camera").performClick()
        compose.onNodeWithContentDescription("Camera").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, calls) }
    }

    @Test
    fun disabledRoundMultiButtonDoesNotExpandOrInvokeItems() {
        var calls = 0
        compose.setContent {
            MaterialTheme {
                RoundMultiButton(
                    icon = Icons.Default.Add,
                    contentDescription = "Add image",
                    enabled = false,
                    items = listOf(RoundMultiButtonItem(Icons.Default.PhotoCamera, { calls++ }, "Camera")),
                )
            }
        }

        compose.onNodeWithContentDescription("Add image").assertIsNotEnabled().performClick()
        compose.onNodeWithContentDescription("Camera").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, calls) }
    }

    @Test
    fun emptyRoundMultiButtonKeepsOnlyTheMainAction() {
        compose.setContent {
            MaterialTheme {
                RoundMultiButton(
                    icon = Icons.Default.Add,
                    contentDescription = "Add image",
                    items = emptyList(),
                )
            }
        }

        compose.onNodeWithContentDescription("Add image").performClick()
        compose.onNodeWithContentDescription("Add image").performClick()
        compose.onAllNodes(hasClickAction()).assertCountEquals(1)
    }

    @Test
    fun removingAllItemsCollapsesAnExpandedRoundMultiButton() {
        var items by mutableStateOf(listOf(RoundMultiButtonItem(Icons.Default.PhotoCamera, {}, "Camera")))
        compose.setContent {
            MaterialTheme {
                RoundMultiButton(
                    icon = Icons.Default.Add,
                    contentDescription = "Add image",
                    items = items,
                )
            }
        }

        compose.onNodeWithContentDescription("Add image").performClick()
        compose.onNodeWithContentDescription("Camera").assertIsDisplayed()
        compose.runOnIdle { items = emptyList() }
        compose.onNodeWithContentDescription("Camera").assertDoesNotExist()
        compose.onAllNodes(hasClickAction()).assertCountEquals(1)
    }

    @Test
    fun disablingAnExpandedRoundMultiButtonCollapsesItsItems() {
        var enabled by mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                RoundMultiButton(
                    icon = Icons.Default.Add,
                    contentDescription = "Add image",
                    enabled = enabled,
                    items = listOf(RoundMultiButtonItem(Icons.Default.PhotoCamera, {}, "Camera")),
                )
            }
        }

        compose.onNodeWithContentDescription("Add image").performClick()
        compose.onNodeWithContentDescription("Camera").assertIsDisplayed()
        compose.runOnIdle { enabled = false }
        compose.onNodeWithContentDescription("Add image").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Camera").assertDoesNotExist()
    }

    @Test
    fun reducedMotionShowsAndHidesItemsWithoutTransitionFrames() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalUiMotionEnabled provides false) {
                MaterialTheme {
                    RoundMultiButton(
                        icon = Icons.Default.Add,
                        contentDescription = "Add image",
                        items = listOf(RoundMultiButtonItem(Icons.Default.PhotoCamera, {}, "Camera")),
                    )
                }
            }
        }

        compose.onNodeWithContentDescription("Add image").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithContentDescription("Camera").assertIsDisplayed()
        compose.onNodeWithContentDescription("Add image").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithContentDescription("Camera").assertDoesNotExist()
    }

    @Test
    fun horizontalStartDirectionPlacesItemsBeforeTheMainButton() {
        compose.setContent {
            MaterialTheme {
                Box(modifier = Modifier.size(300.dp, 100.dp)) {
                    RoundMultiButton(
                        icon = Icons.Default.Add,
                        contentDescription = "Add image",
                        items = listOf(RoundMultiButtonItem(Icons.Default.PhotoCamera, {}, "Camera")),
                        expandAxis = RoundMultiButtonExpandAxis.Horizontal,
                        expandDirection = RoundMultiButtonExpandDirection.Start,
                    )
                }
            }
        }

        compose.onNodeWithContentDescription("Add image").performClick()
        val itemBounds = compose.onNodeWithContentDescription("Camera").getUnclippedBoundsInRoot()
        val mainBounds = compose.onNodeWithContentDescription("Add image").getUnclippedBoundsInRoot()
        assertTrue(itemBounds.right <= mainBounds.left)
    }

    @Test
    fun horizontalEndDirectionPlacesItemsAfterTheMainButton() {
        compose.setContent {
            MaterialTheme {
                Box(modifier = Modifier.size(300.dp, 100.dp)) {
                    RoundMultiButton(
                        icon = Icons.Default.Add,
                        contentDescription = "Add image",
                        items = listOf(RoundMultiButtonItem(Icons.Default.PhotoCamera, {}, "Camera")),
                        expandAxis = RoundMultiButtonExpandAxis.Horizontal,
                        expandDirection = RoundMultiButtonExpandDirection.End,
                    )
                }
            }
        }

        compose.onNodeWithContentDescription("Add image").performClick()
        val itemBounds = compose.onNodeWithContentDescription("Camera").getUnclippedBoundsInRoot()
        val mainBounds = compose.onNodeWithContentDescription("Add image").getUnclippedBoundsInRoot()
        assertTrue(itemBounds.left >= mainBounds.right)
    }

    @Test
    fun horizontalStartDirectionFollowsRightToLeftLayout() {
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme {
                    Box(modifier = Modifier.size(300.dp, 100.dp)) {
                        RoundMultiButton(
                            icon = Icons.Default.Add,
                            contentDescription = "Add image",
                            items = listOf(RoundMultiButtonItem(Icons.Default.PhotoCamera, {}, "Camera")),
                            expandAxis = RoundMultiButtonExpandAxis.Horizontal,
                            expandDirection = RoundMultiButtonExpandDirection.Start,
                        )
                    }
                }
            }
        }

        compose.onNodeWithContentDescription("Add image").performClick()
        val itemBounds = compose.onNodeWithContentDescription("Camera").getUnclippedBoundsInRoot()
        val mainBounds = compose.onNodeWithContentDescription("Add image").getUnclippedBoundsInRoot()
        assertTrue(itemBounds.left >= mainBounds.right)
    }

    @Test
    fun verticalDownDirectionPlacesItemsBelowTheMainButton() {
        compose.setContent {
            MaterialTheme {
                RoundMultiButton(
                    icon = Icons.Default.Add,
                    contentDescription = "Add image",
                    items = listOf(RoundMultiButtonItem(Icons.Default.PhotoCamera, {}, "Camera")),
                    expandDirection = RoundMultiButtonExpandDirection.Down,
                )
            }
        }

        compose.onNodeWithContentDescription("Add image").performClick()
        val itemBounds = compose.onNodeWithContentDescription("Camera").getUnclippedBoundsInRoot()
        val mainBounds = compose.onNodeWithContentDescription("Add image").getUnclippedBoundsInRoot()
        assertTrue(itemBounds.top >= mainBounds.bottom)
    }
}
