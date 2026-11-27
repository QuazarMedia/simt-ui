package app.simt.ui.components.forms

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.delay

@Composable
public fun DebouncedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    debounceMillis: Long = 300L,
    placeholder: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    maxLength: Int? = 1000,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    leadingIcon: ImageVector? = null,
    supportingText: @Composable (() -> Unit)? = null,
    testTag: String? = null,
) {
    var internalValue by remember(value) { mutableStateOf(value) }
    var pendingChange by remember { mutableStateOf<String?>(null) }
    val latestOnValueChange by rememberUpdatedState(onValueChange)

    LaunchedEffect(pendingChange) {
        pendingChange?.let { newValue ->
            delay(debounceMillis)
            latestOnValueChange(newValue)
            pendingChange = null
        }
    }

    ClearableTextField(
        value = internalValue,
        onValueChange = { newValue ->
            internalValue = newValue
            pendingChange = newValue
        },
        label = label,
        modifier = modifier,
        placeholder = placeholder,
        isError = isError,
        singleLine = singleLine,
        maxLength = maxLength,
        keyboardOptions = keyboardOptions,
        leadingIcon = leadingIcon,
        supportingText = supportingText,
        testTag = testTag,
    )
}
