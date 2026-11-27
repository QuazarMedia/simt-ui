package app.simt.ui.components.layout

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import app.simt.ui.effects.LocalUiMotionEnabled
import app.simt.ui.effects.SimtMotion
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AccordionGroupTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun headerAndDisclosureToggleCallerStateAndExposeAccessibleLabels() {
        var expanded by mutableStateOf(false)
        val changes = mutableListOf<Boolean>()
        compose.setContent {
            MaterialTheme {
                AccordionGroup(
                    title = "Deep sky",
                    subtitle = "Observer targets",
                    isExpanded = expanded,
                    onExpandedChange = {
                        changes.add(it)
                        expanded = it
                    },
                    expandLabel = "Expand group",
                    collapseLabel = "Collapse group",
                    itemCount = 3,
                ) {
                    Text("Sirius")
                }
            }
        }

        compose.onNodeWithText("Deep sky").assertIsDisplayed()
        compose.onNodeWithText("Observer targets").assertIsDisplayed()
        compose.onNodeWithText("3").assertExists()
        compose.onNodeWithText("Sirius").assertDoesNotExist()
        compose.onNodeWithContentDescription("Expand group")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Sirius").assertIsDisplayed()
        compose.onNodeWithContentDescription("Collapse group").assertExists()
        compose.onNodeWithText("Deep sky").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Sirius").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf(true, false), changes) }
    }

    @Test
    fun triStateActionDoesNotExpandAndUsesCallerState() {
        var expanded by mutableStateOf(false)
        var triState by mutableStateOf(ToggleableState.On)
        var toggleCalls = 0
        compose.setContent {
            MaterialTheme {
                AccordionGroup(
                    title = "Targets",
                    isExpanded = expanded,
                    onExpandedChange = { expanded = it },
                    expandLabel = "Expand group",
                    collapseLabel = "Collapse group",
                    triState = triState,
                    onToggleAll = { toggleCalls++ },
                ) {
                    Text("Orion")
                }
            }
        }

        compose.onNode(isToggleable()).assertIsOn().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        compose.onNode(isToggleable()).performClick()
        compose.runOnIdle {
            assertEquals(1, toggleCalls)
            assertEquals(false, expanded)
        }
        triState = ToggleableState.Indeterminate
        compose.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Indeterminate),
        ).assertExists()
        compose.onNodeWithText("Orion").assertDoesNotExist()
    }

    @Test
    fun longPressInvokesItsCallbackWithoutExpanding() {
        var expanded by mutableStateOf(false)
        var longPressCalls = 0
        compose.setContent {
            MaterialTheme {
                AccordionGroup(
                    title = "Galaxies",
                    isExpanded = expanded,
                    onExpandedChange = { expanded = it },
                    expandLabel = "Expand group",
                    collapseLabel = "Collapse group",
                    onLongPress = { longPressCalls++ },
                ) {
                    Text("Andromeda")
                }
            }
        }

        compose.onNodeWithText("Galaxies").performTouchInput { longClick() }
        compose.runOnIdle {
            assertEquals(1, longPressCalls)
            assertEquals(false, expanded)
        }
        compose.onNodeWithText("Andromeda").assertDoesNotExist()
        compose.onNodeWithText("Galaxies").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Andromeda").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, longPressCalls) }
    }

    @Test
    fun enabledMotionRetainsContentUntilExitCompletes() {
        compose.mainClock.autoAdvance = false
        var expanded by mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                AccordionGroup(
                    title = "Stars",
                    isExpanded = expanded,
                    onExpandedChange = { expanded = it },
                    expandLabel = "Expand group",
                    collapseLabel = "Collapse group",
                ) {
                    Text("Polaris")
                }
            }
        }

        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Polaris").assertIsDisplayed()
        compose.onNodeWithText("Stars").performClick()
        compose.mainClock.advanceTimeBy(SimtMotion.MEDIUM / 2L)
        compose.onNodeWithText("Polaris").assertExists()
        compose.mainClock.advanceTimeBy(SimtMotion.MEDIUM.toLong())
        compose.onNodeWithText("Polaris").assertDoesNotExist()
    }

    @Test
    fun disabledMotionRemovesCollapsedContentOnTheNextFrame() {
        compose.mainClock.autoAdvance = false
        var expanded by mutableStateOf(true)
        compose.setContent {
            CompositionLocalProvider(LocalUiMotionEnabled provides false) {
                MaterialTheme {
                    AccordionGroup(
                        title = "Stars",
                        isExpanded = expanded,
                        onExpandedChange = { expanded = it },
                        expandLabel = "Expand group",
                        collapseLabel = "Collapse group",
                    ) {
                        Text("Polaris")
                    }
                }
            }
        }

        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Polaris").assertIsDisplayed()
        compose.onNodeWithText("Stars").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Polaris").assertDoesNotExist()
    }

    @Test
    fun countIsOptionalAndExternalModifierIsApplied() {
        compose.setContent {
            MaterialTheme {
                AccordionGroup(
                    title = "Empty group",
                    isExpanded = false,
                    onExpandedChange = {},
                    expandLabel = "Expand group",
                    collapseLabel = "Collapse group",
                    modifier = Modifier.testTag("accordion"),
                    content = {},
                )
            }
        }

        compose.onNodeWithTag("accordion").assertExists()
        compose.onNodeWithText("0").assertDoesNotExist()
    }
}
