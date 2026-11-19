package app.simt.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.simt.ui.components.banners.InformationBanner
import app.simt.ui.components.forms.ClearableTextField
import app.simt.ui.components.forms.DataField

/**
 * Generic single-field prompt. Wraps [ConfirmDialog] and hosts an optional hint banner, a text
 * field, and Save/Cancel actions. Use it whenever you need to ask the user for a single short
 * text value (renaming, setting a default, etc.) instead of building a bespoke dialog or inline
 * editor.
 *
 * With [suggestions] the field is an autocompletion [DataField] (the same field the target form
 * uses for tags): the list shows as a dropdown while typing, and free text is still allowed.
 * Without it the plain [app.simt.ui.components.forms.ClearableTextField] is rendered, so
 * existing callers — including the multi-line ones that rely on [minLines]/[maxLines] — are
 * unaffected.
 *
 * The text is owned internally, seeded from [initialValue] and re-synced whenever [initialValue]
 * changes. Save delivers the raw field value to [onSave] and then triggers [onDismiss], so the
 * caller only owns visibility and persistence (trim/validate on its side as needed).
 *
 * @param title Dialog title.
 * @param initialValue Initial text seeded into the field.
 * @param label Label for the text field.
 * @param onSave Called with the raw field value when the user taps Save (before [onDismiss]).
 * @param onDismiss Called on cancel, scrim tap, back press, and right after [onSave].
 * @param titleIcon Optional icon shown in the title orb.
 * @param hint Optional hint shown in an [InformationBanner] above the field; null hides the banner.
 * @param hintIcon Icon for the hint banner.
 * @param placeholder Optional placeholder for the field.
 * @param leadingIcon Optional leading icon inside the field.
 * @param suggestions Autocompletion list; non-empty switches the field to [DataField].
 * @param maxLength Max input length (null = unbounded).
 * @param singleLine Whether the field is single line.
 * @param minLines Minimum visible field lines (plain field only).
 * @param maxLines Maximum visible field lines (plain field only).
 * @param supportingText Optional field footer that receives the current input (for counters/help).
 * @param keyboardOptions Keyboard options for the field.
 * @param saveLabel Optional Save button label (null = default "Save").
 * @param cancelLabel Optional Cancel button label (null = default "Cancel").
 * @param isSaveEnabled Predicate over the current input controlling Save enablement.
 * @param fieldTestTag Optional test tag for the field.
 */
@Composable
public fun PromptDialog(
    title: String,
    initialValue: String,
    label: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    titleIcon: ImageVector? = null,
    hint: String? = null,
    hintIcon: ImageVector = Icons.Default.Info,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    suggestions: List<String> = emptyList(),
    maxLength: Int? = 1000,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    supportingText: (@Composable (String) -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    saveLabel: String? = null,
    cancelLabel: String? = null,
    isSaveEnabled: (String) -> Boolean = { true },
    fieldTestTag: String? = null,
) {
    var input by remember(initialValue) { mutableStateOf(initialValue) }
    var showHint by remember(hint) { mutableStateOf(true) }

    ConfirmDialog(
        onDismissRequest = onDismiss,
        title = title,
        titleIcon = titleIcon,
        confirmLabel = saveLabel,
        confirmEnabled = isSaveEnabled(input),
        onConfirm = {
            onSave(input)
            onDismiss()
        },
        cancelLabel = cancelLabel,
        onCancel = onDismiss,
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!hint.isNullOrBlank() && showHint) {
                    InformationBanner(
                        text = hint,
                        icon = hintIcon,
                        tintColor = MaterialTheme.colorScheme.primary,
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        onClose = { showHint = false },
                    )
                }
                if (suggestions.isNotEmpty()) {
                    DataField(
                        value = input,
                        onValueChange = { input = it },
                        label = label,
                        suggestions = suggestions,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = placeholder,
                        singleLine = singleLine,
                        maxLength = maxLength,
                        minLines = minLines,
                        maxLines = maxLines,
                        keyboardOptions = keyboardOptions,
                        leadingIcon = leadingIcon,
                        testTag = fieldTestTag,
                        supportingText = supportingText?.let { content -> { content(input) } },
                    )
                } else {
                    ClearableTextField(
                        value = input,
                        onValueChange = { input = it },
                        label = label,
                        placeholder = placeholder,
                        leadingIcon = leadingIcon,
                        singleLine = singleLine,
                        maxLength = maxLength,
                        minLines = minLines,
                        maxLines = maxLines,
                        supportingText = supportingText?.let { content -> { content(input) } },
                        keyboardOptions = keyboardOptions,
                        testTag = fieldTestTag,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    )
}
