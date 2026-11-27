package app.simt.ui.components.forms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class IconPickerPaletteTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun expandingScrollsAnAlreadySelectedOptionPastTheFirstPageIntoView() {
        var expanded by mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                Box(modifier = Modifier.width(120.dp)) {
                    IconPickerChip(
                        expanded = expanded,
                        onClick = { expanded = true },
                        contentDescription = "chip",
                        isDarkTheme = false,
                    ) {
                        Text("chip")
                    }
                    AnimatedVisibility(visible = expanded) {
                        IconPickerPalette(
                            options = (1..10).toList(),
                            selected = 8,
                            onSelected = {},
                            isDarkTheme = false,
                        ) { option, _ ->
                            Text(option.toString())
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("chip").performClick()
        compose.waitForIdle()

        val bounds = compose.onNodeWithText("8").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "the selected option must be scrolled into the visible row, but it sits at $bounds",
            bounds.left < 120 && bounds.right > 0,
        )
    }

    @Test
    fun expandingScrollsTheLastSelectionIntoADialogWidthView() {
        var expanded by mutableStateOf(false)
        val options = (1..20).toList()
        compose.setContent {
            MaterialTheme {
                Box(modifier = Modifier.width(330.dp)) {
                    IconPickerChip(
                        expanded = expanded,
                        onClick = { expanded = true },
                        contentDescription = "chip",
                        isDarkTheme = false,
                    ) {
                        Text("chip")
                    }
                    AnimatedVisibility(visible = expanded) {
                        IconPickerPalette(
                            options = options,
                            selected = options.last(),
                            onSelected = {},
                            isDarkTheme = false,
                        ) { option, _ ->
                            Text(option.toString())
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("chip").performClick()
        compose.waitForIdle()

        val bounds = compose.onNodeWithText(options.last().toString()).fetchSemanticsNode().boundsInRoot
        assertTrue(
            "the last selected option must be scrolled into the visible row, but it sits at $bounds",
            bounds.left < 330 && bounds.right > 0,
        )
    }

    @Test
    fun iconColorNameRowPublishesSelectionsAndTruncatesTheName() {
        val icons = listOf("one", "two")
        val colors = listOf(Color.Red, Color.Blue)
        var selectedIcon by mutableStateOf(icons.first())
        var selectedColor by mutableStateOf(colors.first())
        val name = mutableStateOf("")
        val nameChanges = mutableListOf<String>()
        compose.setContent {
            MaterialTheme {
                IconColorNameRow(
                    name = name.value,
                    onNameChange = {
                        nameChanges.add(it)
                        name.value = it
                    },
                    selectedIcon = selectedIcon,
                    onIconChange = { selectedIcon = it },
                    selectedColor = selectedColor,
                    onColorChange = { selectedColor = it },
                    nameLabel = "Name",
                    iconSelectorContentDescription = "Select icon",
                    colorSelectorContentDescription = "Select color",
                    nameMaxLength = 3,
                    availableIcons = icons,
                    availableColors = colors,
                    colorContentDescription = { color -> if (color == Color.Blue) "Blue" else "Red" },
                    nameTestTag = "name",
                    isDarkTheme = false,
                ) { option, size ->
                    Text(option, modifier = Modifier.size(size))
                }
            }
        }

        compose.onNodeWithTag("name").performTextInput("abcdef")
        compose.onNodeWithTag("name").assert(hasText("abc"))

        compose.onNodeWithContentDescription("Select icon").performClick()
        compose.onNodeWithText("two").performClick()
        compose.runOnIdle { assertEquals("two", selectedIcon) }

        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithContentDescription("Select color").performClick()
        compose.onNodeWithContentDescription("Blue").performClick()
        compose.runOnIdle {
            assertEquals(Color.Blue, selectedColor)
            assertEquals(listOf("abc"), nameChanges)
        }
    }
}
