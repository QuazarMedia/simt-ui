package app.simt.ui.components.forms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * A text field with autocompletion support that extends ClearableTextField.
 * Shows suggestions in a dropdown that can be clicked, or allows typing custom text.
 *
 * @param value Current text value
 * @param onValueChange Callback when text changes
 * @param label Label for the field
 * @param suggestions List of suggestion strings to show
 * @param modifier Modifier for the field
 * @param placeholder Optional placeholder text
 * @param singleLine Whether the field is single line
 * @param maxLength Maximum character length
 * @param minLines Minimum visible lines
 * @param maxLines Maximum visible lines
 * @param keyboardOptions Keyboard options
 * @param leadingIcon Optional leading icon
 * @param testTag Optional test tag
 * @param onSuggestionSelected Optional callback when a suggestion is clicked
 * @param supportingText Optional supporting text composable displayed below the field
 * @param filterSuggestions Function to filter suggestions based on input (default: case-insensitive contains)
 */
@Composable
public fun DataField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suggestions: List<String>,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    maxLength: Int? = null,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    leadingIcon: ImageVector? = null,
    testTag: String? = null,
    onSuggestionSelected: ((String) -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    filterSuggestions: (String, List<String>) -> List<String> = { input, list ->
        if (input.isBlank()) {
            list
        } else {
            list.filter { it.contains(input, ignoreCase = true) }
        }
    },
) {
    var isFocused by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    // Filter suggestions based on current input
    val filteredSuggestions =
        remember(value, suggestions) {
            filterSuggestions(value, suggestions)
        }

    // Show suggestions when focused and there are filtered suggestions
    val shouldShowSuggestions = isFocused && filteredSuggestions.isNotEmpty() && value.isNotBlank()

    Box(modifier = modifier) {
        ClearableTextField(
            value = value,
            onValueChange = { newValue ->
                onValueChange(newValue)
                expanded = true
            },
            label = label,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                        expanded = focusState.isFocused && filteredSuggestions.isNotEmpty() && value.isNotBlank()
                    }
                    .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
            placeholder = placeholder,
            singleLine = singleLine,
            maxLength = maxLength,
            minLines = minLines,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            leadingIcon = leadingIcon,
            supportingText = supportingText,
        )

        // Suggestions dropdown using Popup for better positioning
        if (shouldShowSuggestions && expanded && filteredSuggestions.isNotEmpty()) {
            Popup(
                onDismissRequest = { expanded = false },
                properties =
                    PopupProperties(
                        focusable = false,
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true,
                    ),
            ) {
                Card(
                    modifier =
                        Modifier
                            .widthIn(min = 200.dp, max = 400.dp)
                            .fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        filteredSuggestions.take(5).forEach { suggestion ->
                            Text(
                                text = suggestion,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onValueChange(suggestion)
                                            onSuggestionSelected?.invoke(suggestion)
                                            expanded = false
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}
