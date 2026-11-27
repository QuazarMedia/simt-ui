package app.simt.ui.components.forms

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
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
class ChoiceControlsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun multiChoiceEnforcesTheMinimumAndPublishesEachToggle() {
        val state = AppMultiChoiceState(initialSelected = setOf("a"), minSelected = 1)
        val changes = mutableListOf<Set<String>>()
        compose.setContent {
            MaterialTheme {
                AppMultiChoice(
                    options = listOf(AppMultiChoiceOption("a", "Alpha"), AppMultiChoiceOption("b", "Beta")),
                    state = state,
                    onSelectionChange = changes::add,
                )
            }
        }

        compose.onNodeWithText("Alpha").assertIsSelected()
        compose.onNodeWithText("Alpha").performClick()
        compose.onNodeWithText("Alpha").assertIsSelected()
        compose.onNodeWithText("Beta").performClick()
        compose.onNodeWithText("Beta").assertIsSelected()
        compose.onNodeWithText("Alpha").performClick()
        compose.onNodeWithText("Alpha").assertIsNotSelected()
        compose.onNodeWithText("Beta").performClick()
        compose.onNodeWithText("Beta").assertIsSelected()
        compose.runOnIdle {
            assertEquals(setOf("b"), state.selected)
            assertEquals(listOf(setOf("a"), setOf("a", "b"), setOf("b"), setOf("b")), changes)
        }
    }

    @Test
    fun optionalSingleChoiceClearsTheSelectedOption() {
        val state = AppSingleChoiceState(initialSelected = "a", required = false)
        val changes = mutableListOf<String?>()
        compose.setContent {
            MaterialTheme {
                AppSingleChoice(
                    options = listOf(AppSingleChoiceOption("a", "Alpha"), AppSingleChoiceOption("b", "Beta")),
                    state = state,
                    onSelectionChange = changes::add,
                )
            }
        }

        compose.onNodeWithText("Alpha").performClick()
        compose.onNodeWithText("Alpha").assertIsNotSelected()
        compose.runOnIdle {
            assertEquals(null, state.selected)
            assertEquals(listOf(null), changes)
        }

        compose.onNodeWithText("Beta").performClick()
        compose.onNodeWithText("Beta").assertIsSelected()
        compose.runOnIdle {
            assertEquals("b", state.selected)
            assertEquals(listOf(null, "b"), changes)
        }
    }

    @Test
    fun multiChoiceLongPressReportsWithoutToggling() {
        val state = AppMultiChoiceState(initialSelected = emptySet<String>())
        val changes = mutableListOf<Set<String>>()
        val longPresses = mutableListOf<String>()
        compose.setContent {
            MaterialTheme {
                AppMultiChoice(
                    options = listOf(AppMultiChoiceOption("a", "Alpha")),
                    state = state,
                    onSelectionChange = changes::add,
                    onLongClick = longPresses::add,
                )
            }
        }

        compose.onNodeWithText("Alpha").performTouchInput { longClick() }
        compose.onNodeWithText("Alpha").assertIsNotSelected()
        compose.runOnIdle {
            assertTrue(state.selected.isEmpty())
            assertTrue(changes.isEmpty())
            assertEquals(listOf("a"), longPresses)
        }
    }

    @Test
    fun disabledChoiceDoesNotToggle() {
        val state = AppMultiChoiceState(initialSelected = emptySet<String>())
        val changes = mutableListOf<Set<String>>()
        compose.setContent {
            MaterialTheme {
                AppMultiChoice(
                    options = listOf(AppMultiChoiceOption("disabled", "Disabled", enabled = false)),
                    state = state,
                    onSelectionChange = changes::add,
                )
            }
        }

        compose.onNodeWithText("Disabled").assertIsNotEnabled().performClick()
        compose.runOnIdle {
            assertTrue(state.selected.isEmpty())
            assertTrue(changes.isEmpty())
        }
    }

    @Test
    fun selectFieldShowsTheSelectedValueAndReportsTheChosenOption() {
        val selected = mutableStateOf<String?>("a")
        val changes = mutableListOf<String>()
        compose.setContent {
            MaterialTheme {
                AppSelectField(
                    options = listOf(AppSelectOption("a", "Alpha"), AppSelectOption("b", "Beta")),
                    selected = selected.value,
                    onSelect = {
                        changes.add(it)
                        selected.value = it
                    },
                    label = "Synthetic selection",
                )
            }
        }

        compose.onNodeWithText("Alpha").assertIsDisplayed()
        compose.onNode(hasText("Alpha") and hasClickAction()).performClick()
        compose.onNodeWithContentDescription("Selected").assertIsDisplayed()
        compose.onNodeWithText("Beta").performClick()
        compose.onNodeWithText("Beta").assertIsDisplayed()
        compose.onNodeWithContentDescription("Selected").assertDoesNotExist()
        compose.onNodeWithText("Alpha").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf("b"), changes) }
    }

    @Test
    fun disabledSelectFieldDoesNotOpen() {
        var selections = 0
        compose.setContent {
            MaterialTheme {
                AppSelectField(
                    options = listOf(AppSelectOption("a", "Alpha")),
                    selected = null,
                    onSelect = { selections++ },
                    label = "Synthetic selection",
                    placeholder = "Choose one",
                    enabled = false,
                )
            }
        }

        compose.onNodeWithText("Synthetic selection").assertIsDisplayed().performTouchInput { click() }
        compose.onNodeWithText("Alpha").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, selections) }
    }
}
