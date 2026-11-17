package app.shared.ui.components.forms

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import app.shared.ui.resources.Res
import app.shared.ui.resources.simt_ui_cd_clear
import app.shared.ui.resources.simt_ui_city_name
import app.shared.ui.resources.simt_ui_country_code_label
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
}
