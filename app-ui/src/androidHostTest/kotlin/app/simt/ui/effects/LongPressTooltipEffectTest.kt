package app.simt.ui.effects

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalTestApi::class, ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LongPressTooltipEffectTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun shortTapCallsCurrentCallbackOnceAndLongPressOnlyShowsTooltip() {
        var original = 0
        var replacement = 0
        val onClick = mutableStateOf<() -> Unit>({ original++ })
        compose.setContent {
            MaterialTheme {
                LongPressTooltipEffect(
                    tooltipText = "Tooltip detail",
                    onClick = onClick.value,
                    modifier = Modifier.size(96.dp),
                    contentModifier = Modifier.testTag("target"),
                    fillMaxHeight = false,
                ) { Text("Anchor", Modifier.size(96.dp)) }
            }
        }
        compose.onAllNodes(hasClickAction()).assertCountEquals(1)
        compose.onNodeWithTag("target").performTouchInput { click() }
        compose.onNodeWithText("Tooltip detail").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(1, original)
            assertEquals(0, replacement)
            onClick.value = { replacement++ }
        }
        compose.onNodeWithTag("target").performTouchInput {
            advanceEventTime(500)
            click()
        }
        compose.runOnIdle {
            assertEquals(1, original)
            assertEquals(1, replacement)
        }
        compose.onNodeWithTag("target").performTouchInput { longClick() }
        compose.onNodeWithText("Tooltip detail").assertExists()
        compose.runOnIdle {
            assertEquals(1, original)
            assertEquals(1, replacement)
        }
    }

    @Test
    fun disabledAnchorSuppressesCallbacksAndTooltip() {
        var clicks = 0
        compose.setContent {
            MaterialTheme {
                LongPressTooltipEffect(
                    tooltipText = "Unavailable detail",
                    onClick = { clicks++ },
                    modifier = Modifier.size(96.dp),
                    contentModifier = Modifier.testTag("target"),
                    fillMaxHeight = false,
                    enabled = false,
                    role = Role.Button,
                ) { Text("Anchor", Modifier.size(96.dp)) }
            }
        }

        val target = compose.onNodeWithTag("target")
        target.assertIsNotEnabled().assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button),
        )
        target.performClick()
        target.performTouchInput { longClick() }
        compose.onNodeWithText("Unavailable detail").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, clicks) }
    }

    @Test
    fun keyboardClickAndAccessibleLongClickRemainAvailableWithOneOwner() {
        var clicks = 0
        lateinit var inputMode: InputModeManager
        compose.setContent {
            inputMode = LocalInputModeManager.current
            MaterialTheme {
                LongPressTooltipEffect(
                    tooltipText = "Keyboard detail",
                    onClick = { clicks++ },
                    modifier = Modifier.size(96.dp),
                    contentModifier = Modifier.testTag("target"),
                    fillMaxHeight = false,
                ) { Text("Anchor", Modifier.size(96.dp)) }
            }
        }
        val target = compose.onNodeWithTag("target")
        compose.onAllNodes(hasClickAction()).assertCountEquals(1)
        compose.runOnIdle { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }
        compose.waitForIdle()
        target.performSemanticsAction(SemanticsActions.RequestFocus) { assertTrue(it()) }
        compose.waitForIdle()
        target.performKeyInput { pressKey(Key.Enter) }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1, clicks) }
        target.performSemanticsAction(SemanticsActions.OnLongClick)
        compose.onNodeWithText("Keyboard detail").assertExists()
        compose.runOnIdle { assertEquals(1, clicks) }
    }
}
