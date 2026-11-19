package app.simt.ui.components.forms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import app.simt.ui.resources.Res
import app.simt.ui.resources.simt_ui_cd_clear
import app.simt.ui.resources.simt_ui_city_name
import app.simt.ui.resources.simt_ui_country_code_label
import org.jetbrains.compose.resources.stringResource
import org.junit.Assert.assertEquals
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
class FormsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun inputIsTruncatedToTheRequestedLength() {
        val value = mutableStateOf("")
        val changes = mutableListOf<String>()
        compose.setContent {
            MaterialTheme {
                ClearableTextField(
                    value = value.value,
                    onValueChange = {
                        changes.add(it)
                        value.value = it
                    },
                    label = "Synthetic field",
                    maxLength = 5,
                    testTag = "field",
                )
            }
        }

        compose.onNodeWithTag("field").performTextInput("0123456789")
        compose.onNodeWithTag("field").assert(hasText("01234"))
        compose.runOnIdle {
            assertEquals("01234", value.value)
            assertEquals(listOf("01234"), changes)
        }
    }

    @Test
    fun defaultLengthLimitIsOneThousand() {
        val value = mutableStateOf("")
        val changes = mutableListOf<String>()
        compose.setContent {
            MaterialTheme {
                ClearableTextField(
                    value = value.value,
                    onValueChange = {
                        changes.add(it)
                        value.value = it
                    },
                    label = "Synthetic field",
                    testTag = "field",
                )
            }
        }

        compose.onNodeWithTag("field").performTextInput("x".repeat(1025))
        compose.onNodeWithTag("field").assert(hasText("x".repeat(1000)))
        compose.runOnIdle {
            assertEquals("x".repeat(1000), value.value)
            assertEquals(listOf("x".repeat(1000)), changes)
        }
    }

    @Test
    fun nullLengthLimitAcceptsAllInput() {
        val value = mutableStateOf("")
        val changes = mutableListOf<String>()
        val input = "x".repeat(1025)
        compose.setContent {
            MaterialTheme {
                ClearableTextField(
                    value = value.value,
                    onValueChange = {
                        changes.add(it)
                        value.value = it
                    },
                    label = "Synthetic field",
                    maxLength = null,
                    testTag = "field",
                )
            }
        }

        compose.onNodeWithTag("field").performTextInput(input)
        compose.onNodeWithTag("field").assert(hasText(input))
        compose.runOnIdle {
            assertEquals(input, value.value)
            assertEquals(listOf(input), changes)
        }
    }

    @Test
    fun customClearLabelClearsExactlyOnceAndDisappearsWhenEmpty() {
        val value = mutableStateOf("")
        val changes = mutableListOf<String>()
        val clearLabel = "Reset synthetic field"
        compose.setContent {
            MaterialTheme {
                ClearableTextField(
                    value = value.value,
                    onValueChange = {
                        changes.add(it)
                        value.value = it
                    },
                    label = "Synthetic field",
                    clearContentDescription = clearLabel,
                    testTag = "field",
                )
            }
        }

        compose.onNodeWithContentDescription(clearLabel).assertDoesNotExist()
        compose.onNodeWithTag("field").performTextInput("seed")
        compose.onNodeWithContentDescription(clearLabel).assertExists()
        compose.onNodeWithContentDescription(clearLabel).performTouchInput { click() }
        compose.onNodeWithTag("field").assert(hasText(""))
        compose.onNodeWithContentDescription(clearLabel).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals("", value.value)
            assertEquals(listOf("seed", ""), changes)
        }
    }

    @Test
    fun readOnlyRejectsTypingButAllowsClearWithoutCallingTheBodyCallback() {
        val value = mutableStateOf("seed")
        val changes = mutableListOf<String>()
        var bodyClicks = 0
        val clearLabel = "Reset synthetic field"
        compose.setContent {
            MaterialTheme {
                ClearableTextField(
                    value = value.value,
                    onValueChange = {
                        changes.add(it)
                        value.value = it
                    },
                    label = "Synthetic field",
                    readOnly = true,
                    onClick = { bodyClicks++ },
                    clearContentDescription = clearLabel,
                    testTag = "field",
                )
            }
        }

        compose.onNodeWithTag("field").performTouchInput {
            click(Offset(width * 0.25f, height * 0.5f))
        }
        compose.onNodeWithTag("field").performKeyInput { pressKey(Key.A) }
        compose.onNodeWithTag("field").assert(hasText("seed"))
        compose.runOnIdle {
            assertEquals("seed", value.value)
            assertEquals(emptyList<String>(), changes)
            assertEquals(1, bodyClicks)
        }

        compose.onNodeWithContentDescription(clearLabel).performTouchInput { click() }
        compose.onNodeWithTag("field").assert(hasText(""))
        compose.onNodeWithContentDescription(clearLabel).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals("", value.value)
            assertEquals(listOf(""), changes)
            assertEquals(1, bodyClicks)
        }
    }

    @Test
    fun readOnlyBodyTouchUsesTheReplacedCallback() {
        val value = mutableStateOf("seed")
        val changes = mutableListOf<String>()
        var originalClicks = 0
        var replacementClicks = 0
        val onClick = mutableStateOf<(() -> Unit)?>({ originalClicks++ })
        compose.setContent {
            MaterialTheme {
                ClearableTextField(
                    value = value.value,
                    onValueChange = {
                        changes.add(it)
                        value.value = it
                    },
                    label = "Synthetic field",
                    readOnly = true,
                    onClick = onClick.value,
                    testTag = "field",
                )
            }
        }

        compose.onNodeWithTag("field").performTouchInput {
            click(Offset(width * 0.25f, height * 0.5f))
        }
        compose.runOnIdle {
            assertEquals(1, originalClicks)
            assertEquals(0, replacementClicks)
            onClick.value = { replacementClicks++ }
        }
        compose.onNodeWithTag("field").performTouchInput {
            // Separate body taps must not become a double-tap selection gesture.
            advanceEventTime(500)
            click(Offset(width * 0.25f, height * 0.5f))
        }
        compose.onNodeWithTag("field").assert(hasText("seed"))
        compose.runOnIdle {
            assertEquals(1, originalClicks)
            assertEquals(1, replacementClicks)
            assertEquals("seed", value.value)
            assertEquals(emptyList<String>(), changes)
        }
    }

    @Test
    fun cityIsLimitedToThirtyAndCountryToTwoUppercaseCharacters() {
        val city = mutableStateOf("")
        val country = mutableStateOf("")
        val cityChanges = mutableListOf<String>()
        val countryChanges = mutableListOf<String>()
        var cityLabel = ""
        var countryLabel = ""
        compose.setContent {
            MaterialTheme {
                val resolvedCityLabel = stringResource(Res.string.simt_ui_city_name)
                val resolvedCountryLabel = stringResource(Res.string.simt_ui_country_code_label)
                SideEffect {
                    cityLabel = resolvedCityLabel
                    countryLabel = resolvedCountryLabel
                }
                CityCountryFields(
                    city = city.value,
                    onCityChange = {
                        cityChanges.add(it)
                        city.value = it
                    },
                    countryCode = country.value,
                    onCountryCodeChange = {
                        countryChanges.add(it)
                        country.value = it
                    },
                )
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText(cityLabel).performTextInput("x".repeat(35))
        compose.onNodeWithText(cityLabel).assert(hasText("x".repeat(30)))
        compose.onNodeWithText(countryLabel).performTextInput("abz")
        compose.onNodeWithText(countryLabel).assert(hasText("AB"))
        compose.runOnIdle {
            assertEquals("x".repeat(30), city.value)
            assertEquals("AB", country.value)
            assertEquals(listOf("x".repeat(30)), cityChanges)
            assertEquals(listOf("AB"), countryChanges)
        }
    }

    @Test
    fun defaultClearLabelMatchesTheComposeResource() {
        val value = mutableStateOf("seed")
        val changes = mutableListOf<String>()
        var clearLabel = ""
        compose.setContent {
            MaterialTheme {
                val resolvedClearLabel = stringResource(Res.string.simt_ui_cd_clear)
                SideEffect { clearLabel = resolvedClearLabel }
                ClearableTextField(
                    value = value.value,
                    onValueChange = {
                        changes.add(it)
                        value.value = it
                    },
                    label = "Synthetic field",
                    testTag = "field",
                )
            }
        }
        compose.waitForIdle()

        compose.onNodeWithContentDescription(clearLabel).assertExists()
        compose.onNodeWithContentDescription(clearLabel).performTouchInput { click() }
        compose.onNodeWithTag("field").assert(hasText(""))
        compose.onNodeWithContentDescription(clearLabel).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals("", value.value)
            assertEquals(listOf(""), changes)
        }
    }

    @Test
    fun stepperIsReadOnlyWhenTapToEditDisabled() {
        val value = mutableStateOf(10)
        val changes = mutableListOf<Int>()
        compose.setContent {
            MaterialTheme {
                NumberStepperField(
                    label = "Synthetic stepper",
                    value = value.value,
                    onValueChange = {
                        changes.add(it)
                        value.value = it
                    },
                    min = 5,
                    max = 20,
                    debounceMs = 0,
                    tapToEdit = false,
                )
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText("10").performTouchInput {
            click(Offset(width * 0.5f, height * 0.5f))
        }
        compose.onNodeWithText("10").performKeyInput { pressKey(Key.A) }
        compose.onNodeWithText("10").assertExists()
        compose.runOnIdle {
            assertEquals(10, value.value)
            assertEquals(emptyList<Int>(), changes)
        }
    }

    @Test
    fun tapToEditParsesCoercesAndPropagatesTypedValues() {
        val value = mutableStateOf(10)
        val changes = mutableListOf<Int>()
        compose.setContent {
            MaterialTheme {
                NumberStepperField(
                    label = "Synthetic stepper",
                    value = value.value,
                    onValueChange = {
                        changes.add(it)
                        value.value = it
                    },
                    min = 5,
                    max = 20,
                    debounceMs = 0,
                    tapToEdit = true,
                    modifier = Modifier.padding(4.dp).testTag("stepper"),
                )
            }
        }
        compose.waitForIdle()

        // Tap the value to focus it (caret lands at the end), empty the field with
        // two backspaces, then type a replacement: the parsed value is coerced
        // into the 5..20 range while the field is focused.
        compose.onNodeWithTag("stepper").performTouchInput {
            click(Offset(width * 0.5f, height * 0.5f))
        }
        compose.onNodeWithTag("stepper").performKeyInput {
            pressKey(Key.Backspace)
            pressKey(Key.Backspace)
            pressKey(Key.Two)
            pressKey(Key.Five)
        }
        compose.onNodeWithText("25").assertExists()
        // Commit the edit with the IME action; on blur the text normalizes to
        // the coerced value. Intermediate keystrokes are coerced too ("1" -> 5).
        compose.onNodeWithTag("stepper").performImeAction()
        compose.onNodeWithText("20").assertExists()
        compose.runOnIdle {
            assertEquals(20, value.value)
            // Intermediate keystrokes are coerced into the range as they are
            // typed: "1" -> 5, "2" -> 5, "25" -> 20.
            assertEquals(listOf(5, 5, 20), changes)
        }
    }

    @Test
    fun outOfRangeTypedValueSnapsToTheNearestBoundOnBlur() {
        val value = mutableStateOf(100)
        val changes = mutableListOf<Int>()
        compose.setContent {
            MaterialTheme {
                NumberStepperField(
                    label = "Synthetic stepper",
                    value = value.value,
                    onValueChange = {
                        changes.add(it)
                        value.value = it
                    },
                    min = 50,
                    max = 200,
                    debounceMs = 0,
                    tapToEdit = true,
                    modifier = Modifier.padding(4.dp).testTag("stepper"),
                )
            }
        }
        compose.waitForIdle()

        // Tap to focus, empty the field with three backspaces, and type a
        // value below the minimum: the raw text stays visible while focused.
        compose.onNodeWithTag("stepper").performTouchInput {
            click(Offset(width * 0.5f, height * 0.5f))
        }
        compose.onNodeWithTag("stepper").performKeyInput {
            pressKey(Key.Backspace)
            pressKey(Key.Backspace)
            pressKey(Key.Backspace)
            pressKey(Key.Two)
            pressKey(Key.Zero)
        }
        compose.onNodeWithText("20").assertExists()
        // Commit the edit with the IME action; on focus loss the text snaps
        // to the nearest bound (the minimum, 50) and the coerced value
        // propagates to the parent.
        compose.onNodeWithTag("stepper").performImeAction()
        compose.onNodeWithText("50").assertExists()
        compose.runOnIdle {
            assertEquals(50, value.value)
            // Every intermediate keystroke that parses ("10" -> 50, "1" -> 50,
            // "2" -> 50, "20" -> 50) coerces into the range as it is typed.
            assertEquals(listOf(50, 50, 50, 50), changes)
        }
    }

    @Test
    fun unboundedStepperHasNoEdgeHintsAndPassesTypedValuesThroughUncoerced() {
        val value = mutableStateOf(1000)
        val changes = mutableListOf<Int>()
        compose.setContent {
            MaterialTheme {
                NumberStepperField(
                    label = "Synthetic stepper",
                    value = value.value,
                    onValueChange = {
                        changes.add(it)
                        value.value = it
                    },
                    tapToEdit = true,
                    debounceMs = 0,
                    modifier = Modifier.padding(4.dp).testTag("stepper"),
                )
            }
        }
        compose.waitForIdle()

        // Unbounded mode renders no min/max edge hints.
        compose.onNodeWithText("1000 ≤").assertDoesNotExist()
        compose.onNodeWithText("≤ 1000").assertDoesNotExist()

        // Tap to focus, empty the field with four backspaces, and type an
        // arbitrary value: without a range it passes through uncoerced.
        compose.onNodeWithTag("stepper").performTouchInput {
            click(Offset(width * 0.5f, height * 0.5f))
        }
        compose.onNodeWithTag("stepper").performKeyInput {
            pressKey(Key.Backspace)
            pressKey(Key.Backspace)
            pressKey(Key.Backspace)
            pressKey(Key.Backspace)
            pressKey(Key.Seven)
            pressKey(Key.Five)
        }
        compose.onNodeWithText("75").assertExists()
        compose.onNodeWithTag("stepper").performImeAction()
        compose.onNodeWithText("75").assertExists()
        compose.runOnIdle {
            assertEquals(75, value.value)
            // Every intermediate keystroke that parses propagates as-is:
            // "100" -> 100, "10" -> 10, "1" -> 1, "7" -> 7, "75" -> 75.
            assertEquals(listOf(100, 10, 1, 7, 75), changes)
        }

        // The stepper buttons stay enabled below any lower bound: stepping down
        // from 75 works even though there is no minimum.
        compose.onNodeWithContentDescription("Delete").performTouchInput { click() }
        compose.onNodeWithText("74").assertExists()
        compose.runOnIdle {
            assertEquals(listOf(100, 10, 1, 7, 75, 74), changes)
        }
    }

    @Test
    fun oneSidedMinCoercesTypedValuesUpwardWithoutAnUpperHint() {
        val value = mutableStateOf(100)
        val changes = mutableListOf<Int>()
        compose.setContent {
            MaterialTheme {
                NumberStepperField(
                    label = "Synthetic stepper",
                    value = value.value,
                    onValueChange = {
                        changes.add(it)
                        value.value = it
                    },
                    min = 50,
                    tapToEdit = true,
                    debounceMs = 0,
                    modifier = Modifier.padding(4.dp).testTag("stepper"),
                )
            }
        }
        compose.waitForIdle()

        // Only the minimum hint is rendered; there is no maximum edge hint.
        compose.onNodeWithText("50 ≤").assertExists()
        compose.onNodeWithText("≤ 100").assertDoesNotExist()

        // Tap into the value region (right of the minimum hint) so the caret lands at
        // the end of the text; empty it, then type a value below the minimum. It
        // coerces upward as it is typed.
        compose.onNodeWithTag("stepper").performTouchInput {
            click(Offset(width * 0.7f, height * 0.5f))
        }
        compose.onNodeWithTag("stepper").performKeyInput {
            pressKey(Key.Backspace)
            pressKey(Key.Backspace)
            pressKey(Key.Backspace)
            pressKey(Key.Two)
            pressKey(Key.Zero)
        }
        compose.onNodeWithTag("stepper").performImeAction()
        compose.onNodeWithText("50").assertExists()
        compose.runOnIdle {
            assertEquals(50, value.value)
            // Every intermediate keystroke that parses ("10" -> 50, "1" -> 50,
            // "2" -> 50, "20" -> 50) coerces up to the minimum as it is typed.
            assertEquals(listOf(50, 50, 50, 50), changes)
        }

        // Without an upper bound the increment button stays enabled at any value.
        compose.onNodeWithContentDescription("Add").performTouchInput { click() }
        compose.onNodeWithText("51").assertExists()
        compose.runOnIdle {
            assertEquals(51, value.value)
        }
    }

    @Test
    fun unfinishedEditRevertsToTheLastValidValueOnBlur() {
        val value = mutableStateOf(10)
        val changes = mutableListOf<Int>()
        compose.setContent {
            MaterialTheme {
                NumberStepperField(
                    label = "Synthetic stepper",
                    value = value.value,
                    onValueChange = {
                        changes.add(it)
                        value.value = it
                    },
                    min = 5,
                    max = 20,
                    debounceMs = 0,
                    tapToEdit = true,
                    modifier = Modifier.padding(4.dp),
                )
                Box(
                    modifier =
                        Modifier
                            .size(24.dp)
                            .focusTarget()
                            .clickable {}
                            .testTag("blur-target"),
                )
            }
        }
        compose.waitForIdle()

        // Focus and empty the field: the test IME leaves the caret at the start,
        // so two forward deletes remove "10".
        compose.onNodeWithText("10").performTouchInput {
            click(Offset(width * 0.5f, height * 0.5f))
        }
        compose.onNodeWithText("10").performKeyInput {
            pressKey(Key.Delete)
            pressKey(Key.Delete)
        }
        compose.onNodeWithTag("blur-target").performTouchInput { click() }
        compose.onNodeWithText("10").assertExists()
        compose.runOnIdle {
            assertEquals(10, value.value)
            assertEquals(emptyList<Int>(), changes)
        }
    }
}
