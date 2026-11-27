package app.simt.ui.components.buttons

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
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
class IconLabelButtonsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun iconLabelButtonUppercasesCaptionAndInvokesActionOnce() {
        var calls = 0
        compose.setContent {
            MaterialTheme {
                IconLabelButton(
                    onClick = { calls++ },
                    icon = Icons.Default.Info,
                    label = "Map",
                    color = Color.Red,
                )
            }
        }

        compose.onNodeWithText("MAP").performClick()
        compose.runOnIdle { assertEquals(1, calls) }
    }

    @Test
    fun disabledIconLabelButtonDoesNotInvokeAction() {
        var calls = 0
        compose.setContent {
            MaterialTheme {
                IconLabelButton(
                    onClick = { calls++ },
                    icon = Icons.Default.Info,
                    label = "Map",
                    color = Color.Red,
                    enabled = false,
                )
            }
        }

        compose.onNodeWithText("MAP").assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle { assertEquals(0, calls) }
    }

    @Test
    fun premiumButtonKeepsOneActionWhenRedModeChanges() {
        var redMode by mutableStateOf(false)
        var calls = 0
        compose.setContent {
            MaterialTheme {
                PremiumIconLabelButton(
                    onClick = {
                        calls++
                        redMode = !redMode
                    },
                    icon = Icons.Default.Info,
                    label = "Connect",
                    isRedMode = redMode,
                )
            }
        }

        compose.onNodeWithText("CONNECT").performClick()
        compose.onNodeWithText("CONNECT").performClick()
        compose.runOnIdle { assertEquals(2, calls) }
    }

    @Test
    fun premiumButtonHonorsCompactSizing() {
        compose.setContent {
            MaterialTheme {
                Row {
                    PremiumIconLabelButton(
                        onClick = {},
                        icon = Icons.Default.Info,
                        label = "Connect",
                        modifier = Modifier.testTag("normal"),
                    )
                    PremiumIconLabelButton(
                        onClick = {},
                        icon = Icons.Default.Info,
                        label = "Connect",
                        compact = true,
                        modifier = Modifier.testTag("compact"),
                    )
                }
            }
        }

        val normalBounds = compose.onNodeWithTag("normal").getUnclippedBoundsInRoot()
        val compactBounds = compose.onNodeWithTag("compact").getUnclippedBoundsInRoot()
        assertTrue((compactBounds.bottom - compactBounds.top) < (normalBounds.bottom - normalBounds.top))
    }

    @Test
    fun iconLabelRowShowsTooltipWithoutInvokingAction() {
        var calls = 0
        compose.setContent {
            MaterialTheme {
                IconLabelButtonRow(
                    actions = listOf(action("map", "Map") { calls++ }),
                    modifier = Modifier.width(320.dp).height(64.dp),
                )
            }
        }

        compose.onAllNodes(hasClickAction()).assertCountEquals(1)
        compose.onNodeWithText("MAP").performClick()
        compose.onNodeWithText("MAP").performTouchInput { longClick() }
        compose.onNodeWithText("Map").assertExists()
        compose.runOnIdle { assertEquals(1, calls) }
    }

    @Test
    fun iconLabelRowCanSuppressTooltip() {
        compose.setContent {
            MaterialTheme {
                IconLabelButtonRow(
                    actions =
                        listOf(
                            action("map", "Map", tooltipText = "") {},
                        ),
                    modifier = Modifier.width(320.dp).height(64.dp),
                )
            }
        }

        compose.onNodeWithText("MAP").performTouchInput { longClick() }
        compose.onNodeWithText("Map").assertDoesNotExist()
    }

    @Test
    fun iconLabelRowPublishesSelectedState() {
        compose.setContent {
            MaterialTheme {
                IconLabelButtonRow(
                    actions = listOf(action("map", "Map", isSelected = true) {}),
                    modifier = Modifier.width(320.dp).height(64.dp),
                )
            }
        }

        compose.onNodeWithText("MAP").assertIsSelected()
    }

    @Test
    fun disabledRowActionDoesNotInvokeAction() {
        var calls = 0
        compose.setContent {
            MaterialTheme {
                IconLabelButtonRow(
                    actions = listOf(action("map", "Map", enabled = false) { calls++ }),
                    modifier = Modifier.width(320.dp).height(64.dp),
                )
            }
        }

        compose.onNodeWithText("MAP").assertIsNotEnabled().performTouchInput { click() }
        compose.onNodeWithText("MAP").performTouchInput { longClick() }
        compose.onNodeWithText("Map").assertExists()
        compose.runOnIdle { assertEquals(0, calls) }
    }

    @Test
    fun iconLabelRowScrollsOnlyWhenActionsOverflow() {
        var overflow by mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                IconLabelButtonRow(
                    actions =
                        (0 until if (overflow) 8 else 2).map { index ->
                            action("action-$index", "Action $index") {}
                        },
                    modifier = Modifier.width(320.dp).height(64.dp),
                    itemSpacing = 0.dp,
                )
            }
        }

        compose.onNode(hasScrollAction()).assertExists()
        compose.runOnIdle { overflow = false }
        compose.onNode(hasScrollAction()).assertDoesNotExist()
    }

    @Test
    fun iconLabelRowReportsActionPosition() {
        var positioned = false
        compose.setContent {
            MaterialTheme {
                IconLabelButtonRow(
                    actions =
                        listOf(
                            action("map", "Map", onPositioned = { positioned = true }) {},
                        ),
                    modifier = Modifier.width(320.dp).height(64.dp),
                )
            }
        }

        compose.runOnIdle { assertTrue(positioned) }
    }

    @Test
    fun toolbarRendersSlotsAndInvokesCentralAction() {
        var calls = 0
        compose.setContent {
            MaterialTheme {
                IconLabelActionsToolbar(
                    actions = listOf(action("map", "Map") { calls++ }),
                    prefixContent = { Text("Before") },
                    suffixContent = { Text("After") },
                )
            }
        }

        compose.onNodeWithText("Before").assertExists()
        compose.onNodeWithText("After").assertExists()
        compose.onNodeWithText("MAP").performClick()
        compose.runOnIdle { assertEquals(1, calls) }
    }

    private fun action(
        key: String,
        label: String,
        enabled: Boolean = true,
        isSelected: Boolean = false,
        tooltipText: String? = null,
        onPositioned: ((androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
        onClick: () -> Unit,
    ): IconLabelAction =
        IconLabelAction(
            key = key,
            icon = Icons.Default.Info,
            label = label,
            onClick = onClick,
            enabled = enabled,
            isSelected = isSelected,
            tooltipText = tooltipText,
            onPositioned = onPositioned,
        )
}
