package app.simt.ui.components.buttons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.simt.ui.resources.Res
import app.simt.ui.resources.simt_ui_cancel
import app.simt.ui.resources.simt_ui_close
import app.simt.ui.resources.simt_ui_save
import org.jetbrains.compose.resources.stringResource

/**
 * Reusable cancel button with inverse primary color styling.
 *
 * @param onClick Callback when the button is clicked
 * @param modifier Optional modifier for the button
 * @param label Optional custom text (defaults to "Cancel")
 * @param enabled Whether the button is enabled
 */
@Composable
public fun CancelLinkButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    icon: ImageVector = Icons.Default.Close,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label ?: stringResource(Res.string.simt_ui_cancel))
    }
}

/**
 * Reusable close button with inverse primary color styling.
 *
 * @param onClick Callback when the button is clicked
 * @param modifier Optional modifier for the button
 * @param enabled Whether the button is enabled
 */
@Composable
public fun CloseLinkButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(Res.string.simt_ui_close))
    }
}

/**
 * Reusable save button with primary color styling.
 *
 * @param onClick Callback when the button is clicked
 * @param modifier Optional modifier for the button
 * @param label Optional custom text (defaults to "Save")
 * @param enabled Whether the button is enabled
 * @param colors Optional button colors (defaults to primary color, use for delete actions with error color)
 */
@Composable
public fun SaveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    icon: ImageVector = Icons.Default.Check,
    enabled: Boolean = true,
    colors: ButtonColors? = null,
) {
    Button(
        onClick = onClick,
        modifier =
            modifier
                .testTag("modal_button_save")
                .heightIn(min = 48.dp),
        enabled = enabled,
        colors = colors ?: ButtonDefaults.buttonColors(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label ?: stringResource(Res.string.simt_ui_save))
    }
}

/**
 * Action buttons row for modals and dialogs.
 * Handles common patterns: Cancel+Save, or Close.
 *
 * @param onCancel Optional cancel action
 * @param onSave Optional save action
 * @param onClose Optional close action
 * @param isSaveEnabled Whether save button is enabled
 * @param cancelLabel Optional text for the cancel button
 * @param saveLabel Optional text for the save button
 * @param modifier Optional modifier for the row
 * @param customActions Optional custom action buttons
 */
@Composable
public fun DialogActionButtons(
    onCancel: (() -> Unit)? = null,
    onSave: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    isSaveEnabled: Boolean = true,
    cancelLabel: String? = null,
    saveLabel: String? = null,
    modifier: Modifier = Modifier,
    customActions: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
    ) {
        if (customActions != null) {
            customActions()
        } else if (onClose != null) {
            CloseLinkButton(onClick = onClose)
        } else if (onSave != null) {
            if (onCancel != null) {
                CancelLinkButton(
                    onClick = onCancel,
                    label = cancelLabel,
                    modifier = Modifier.testTag("modal_button_cancel"),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            SaveButton(
                onClick = onSave,
                label = saveLabel,
                enabled = isSaveEnabled,
            )
        }
    }
}
