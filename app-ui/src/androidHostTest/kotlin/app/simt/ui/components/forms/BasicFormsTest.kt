package app.simt.ui.components.forms

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
class BasicFormsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun debouncedFieldPublishesOnlyTheLatestInputAfterTheDelay() {
        compose.mainClock.autoAdvance = false
        val changes = mutableListOf<String>()
        compose.setContent {
            MaterialTheme {
                DebouncedTextField(
                    value = "",
                    onValueChange = changes::add,
                    label = "Synthetic debounced field",
                    debounceMillis = 300L,
                    testTag = "debounced-field",
                )
            }
        }

        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("debounced-field").performTextInput("a")
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(150)
        compose.onNodeWithTag("debounced-field").performTextInput("b")
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(200)
        compose.runOnIdle { assertTrue(changes.isEmpty()) }

        compose.mainClock.advanceTimeBy(200)
        compose.mainClock.advanceTimeByFrame()
        compose.runOnIdle { assertEquals(listOf("ab"), changes) }
    }

    @Test
    fun debouncedFieldUsesTheReplacedCallbackWithoutRestartingTheDelay() {
        compose.mainClock.autoAdvance = false
        val callback = mutableStateOf<(String) -> Unit>({})
        val originalChanges = mutableListOf<String>()
        val replacementChanges = mutableListOf<String>()
        compose.setContent {
            MaterialTheme {
                DebouncedTextField(
                    value = "",
                    onValueChange = callback.value,
                    label = "Synthetic debounced field",
                    debounceMillis = 300L,
                    testTag = "debounced-field",
                )
            }
        }

        callback.value = { originalChanges.add(it) }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("debounced-field").performTextInput("pending")
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(100)
        compose.runOnIdle { callback.value = { replacementChanges.add(it) } }
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(150)
        compose.runOnIdle { assertTrue(replacementChanges.isEmpty()) }
        compose.mainClock.advanceTimeBy(100)
        compose.mainClock.advanceTimeByFrame()

        compose.runOnIdle {
            assertTrue(originalChanges.isEmpty())
            assertEquals(listOf("pending"), replacementChanges)
        }
    }

    @Test
    fun radioOptionRowAndButtonEachSelectExactlyOnce() {
        val selected = mutableStateOf(false)
        var selections = 0
        compose.setContent {
            MaterialTheme {
                RadioOptionLine(
                    selected = selected.value,
                    label = "Synthetic option",
                    onSelect = {
                        selections++
                        selected.value = !selected.value
                    },
                    testTag = "option-radio",
                ) {
                    Text("Expanded details")
                }
            }
        }

        compose.onNodeWithText("Expanded details").assertDoesNotExist()
        compose.onNodeWithText("Synthetic option").performClick()
        compose.onNodeWithText("Expanded details").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, selections) }

        compose.onNodeWithTag("option-radio").performClick()
        compose.onNodeWithText("Expanded details").assertDoesNotExist()
        compose.onNode(isSelectable()).assertIsNotSelected()
        compose.runOnIdle { assertEquals(2, selections) }
    }

    @Test
    fun settingLineGrantsPermissionThenExposesTheSwitch() {
        val permissionGranted = mutableStateOf(false)
        val checked = mutableStateOf(true)
        var grantClicks = 0
        var checkedChanges = 0
        compose.setContent {
            MaterialTheme {
                SettingLine(
                    label = "Synthetic setting",
                    checked = checked.value,
                    onCheckedChange = {
                        checkedChanges++
                        checked.value = it
                    },
                    supportingText = "Synthetic supporting text",
                    labelBadge = { Text("Beta") },
                    permissionGranted = permissionGranted.value,
                    onGrantPermissionClick = { grantClicks++ },
                    grantPermissionLabel = "Grant permission",
                )
            }
        }

        compose.onNodeWithText("Beta").assertIsDisplayed()
        compose.onNodeWithText("Synthetic supporting text").assertIsDisplayed()
        compose.onNodeWithText("Grant permission")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
            .performClick()
        compose.runOnIdle {
            assertEquals(1, grantClicks)
            assertEquals(0, checkedChanges)
        }

        permissionGranted.value = true
        compose.onNodeWithText("Grant permission").assertDoesNotExist()
        compose.onNode(isToggleable()).assertIsDisplayed()
        compose.onNode(isToggleable()).performClick()
        compose.onNode(isToggleable()).assertIsOff()
        compose.runOnIdle {
            assertEquals(1, grantClicks)
            assertEquals(1, checkedChanges)
        }
    }

    @Test
    fun disabledSettingLineDoesNotInvokeItsCallback() {
        var checkedChanges = 0
        compose.setContent {
            MaterialTheme {
                SettingLine(
                    label = "Disabled synthetic setting",
                    checked = true,
                    onCheckedChange = { checkedChanges++ },
                    enabled = false,
                )
            }
        }

        compose.onNode(isToggleable()).assertIsNotEnabled().performClick()
        compose.runOnIdle { assertEquals(0, checkedChanges) }
    }
}
