package app.shared.ui.components.forms

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.VisualTransformation
import app.shared.ui.resources.Res
import app.shared.ui.resources.simt_ui_cd_clear
import org.jetbrains.compose.resources.stringResource

@Composable
public fun ClearableTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    maxLength: Int? = 1000,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    leadingIcon: ImageVector? = null,
    supportingText: @Composable (() -> Unit)? = null,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    clearContentDescription: String = stringResource(Res.string.simt_ui_cd_clear),
    testTag: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val latestOnClick = rememberUpdatedState(onClick)

    if (onClick != null) {
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                if (interaction is PressInteraction.Release) {
                    latestOnClick.value?.invoke()
                }
            }
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            val newValue = if (maxLength != null) input.take(maxLength) else input
            onValueChange(newValue)
        },
        label = { Text(label) },
        placeholder =
            if (placeholder != null) {
                { Text(placeholder) }
            } else {
                null
            },
        modifier = modifier.then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        isError = isError,
        singleLine = singleLine,
        readOnly = readOnly,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        interactionSource = interactionSource,
        leadingIcon =
            if (leadingIcon != null) {
                {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                null
            },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = clearContentDescription,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        supportingText = supportingText,
    )
}
