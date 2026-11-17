package app.shared.ui.components.forms

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FormsDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun clearActionHasMinimumTargetAndFieldCanBeEditedAgain() {
        var value by mutableStateOf("Initial")
        var calls = 0
        compose.setContent {
            MaterialTheme {
                ClearableTextField(
                    value = value,
                    onValueChange = {
                        value = it
                        calls++
                    },
                    label = "Example",
                    clearContentDescription = "Clear example",
                    testTag = "field",
                )
            }
        }
        val clear = compose.onNodeWithContentDescription("Clear example")
        clear.assertHasClickAction()
        val node = clear.fetchSemanticsNode()
        val bounds = node.touchBoundsInRoot
        val minimum = with(compose.density) { 48.dp.toPx() }
        assertTrue("Touch width ${bounds.width}px is below $minimum px", bounds.width >= minimum)
        assertTrue("Touch height ${bounds.height}px is below $minimum px", bounds.height >= minimum)
        val edgeInset = with(compose.density) { 1.dp.toPx() }
        clear.performTouchInput {
            click(Offset(bounds.left - node.boundsInRoot.left + edgeInset, height * 0.5f))
        }
        compose.runOnIdle {
            assertEquals("", value)
            assertEquals(1, calls)
        }
        compose.onNodeWithContentDescription("Clear example").assertDoesNotExist()
        compose.onNodeWithTag("field").performTextInput("Edited")
        compose.onNodeWithTag("field").assertTextContains("Edited")
    }

    @Test
    fun defaultTranslationIsPackagedAndClearRemainsAvailableWhenReadOnly() {
        var value by mutableStateOf("Example")
        compose.setContent {
            MaterialTheme {
                ClearableTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = "Example",
                    readOnly = true,
                    testTag = "field",
                )
            }
        }
        compose.onNodeWithContentDescription("Clear text").performClick()
        compose.runOnIdle { assertEquals("", value) }
        compose.onNodeWithContentDescription("Clear text").assertDoesNotExist()
    }

    @Test
    fun localizedFieldsPreserveLengthAndCountryNormalizationContracts() {
        var city by mutableStateOf("")
        var country by mutableStateOf("")
        compose.setContent {
            MaterialTheme {
                CityCountryFields(city, { city = it }, country, { country = it })
            }
        }
        compose.onNode(hasSetTextAction() and hasText("City name"))
            .performTextInput("A".repeat(35))
        compose.onNode(hasSetTextAction() and hasText("Country Code (2 letters)"))
            .performTextInput("fra")
        compose.runOnIdle {
            assertEquals("A".repeat(30), city)
            assertEquals("FR", country)
        }
    }
}
