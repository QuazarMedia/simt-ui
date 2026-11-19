package app.simt.ui.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.simt.ui.components.GradientDivider
import app.simt.ui.resources.Res
import app.simt.ui.resources.simt_ui_cancel
import app.simt.ui.resources.simt_ui_confirm
import app.simt.ui.resources.simt_ui_delete
import org.jetbrains.compose.resources.stringResource

/**
 * One row in a [ActionListDialog].
 *
 * @param label The user-facing action label. Doubles as the title of the [ConfirmDialog]
 *   shown when [confirmMessage] is non-null.
 * @param icon Leading icon shown alongside the label. Also reused as the [ConfirmDialog]
 *   title icon and confirm-button icon for the Yes/No prompt, so the user keeps a visual
 *   anchor to the action they tapped.
 * @param isDestructive When true the row is tinted with the error colour to signal that the
 *   action is non-undoable. Combine with [confirmMessage] to require an explicit confirmation.
 *   The confirmation dialog also uses the destructive (red) confirm-button styling in this case,
 *   matching the standalone "Delete Target" confirm dialog.
 * @param confirmMessage When non-null, tapping the action swaps to a [ConfirmDialog] prompt
 *   showing this message before [onClick] is invoked. Use for destructive bulk operations like
 *   "Delete all" so the user can't fire them by accident from a long-press.
 * @param confirmActionLabel Optional override for the confirm-button label in the prompt. When
 *   null we fall back to "Delete" for destructive actions and "Confirm" for the rest, mirroring
 *   the standalone delete-target dialog.
 * @param enabled When false the row renders dimmed and ignores taps. Useful for hiding actions
 *   that don't make sense for the current selection (e.g. "Add to favorites" when every item
 *   is already a favorite) without making the action list shrink/grow between invocations.
 * @param dismissOnSelect When true (default) the dialog closes right after [onClick] fires. Set to
 *   false for actions that layer their own follow-up on top of the list (e.g. a prompt for extra
 *   input) so the list stays mounted underneath and the caller decides when to finally dismiss.
 * @param onClick Invoked when the action is confirmed.
 */
@Immutable
public data class ActionListItem(
    public val label: String,
    public val icon: ImageVector,
    public val isDestructive: Boolean = false,
    public val confirmMessage: String? = null,
    public val confirmActionLabel: String? = null,
    public val enabled: Boolean = true,
    public val dismissOnSelect: Boolean = true,
    public val onClick: () -> Unit,
)

/**
 * Generic Material 3 dialog that presents a vertical list of [ActionListItem]s. Designed for
 * "apply to all items in this group" surfaces (long-press on an accordion header, multi-select
 * confirm sheet, etc.) — kept domain-agnostic so it can be reused outside the targets feature.
 *
 * Destructive actions with a [ActionListItem.confirmMessage] open a separate [ConfirmDialog] before
 * firing, styled identically to the standalone "Delete Target" confirm dialog (red destructive
 * SaveButton + CancelLinkButton) so all destructive prompts in the app feel like one component
 * regardless of how the user reached them. Non-destructive actions with a confirm message use
 * the same dialog but with the regular confirm-button styling.
 */
@Composable
public fun ActionListDialog(
    title: String,
    actions: List<ActionListItem>,
    onDismiss: () -> Unit,
) {
    // Tracks the action awaiting confirmation. Null = list view only; non-null = a ConfirmDialog
    // is layered on top. The bulk-action list AlertDialog stays mounted underneath so the user
    // can dismiss the confirm prompt and still see the rest of the actions.
    var pendingConfirm by remember { mutableStateOf<ActionListItem?>(null) }

    SimtDialog(
        onDismissRequest = onDismiss,
        variant = SimtDialogVariant.Alert,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            BulkActionList(
                actions = actions,
                onActionSelected = { action ->
                    if (action.confirmMessage != null) {
                        pendingConfirm = action
                    } else {
                        action.onClick()
                        if (action.dismissOnSelect) onDismiss()
                    }
                },
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.simt_ui_cancel))
            }
        },
    )

    pendingConfirm?.let { pending ->
        // Resolve the confirm-button label: caller override > "Delete" for destructive >
        // "Confirm" for everything else. Keeps the bulk-delete and singular delete-target
        // dialogs visually identical on the destructive path.
        val confirmLabel =
            pending.confirmActionLabel
                ?: stringResource(if (pending.isDestructive) Res.string.simt_ui_delete else Res.string.simt_ui_confirm)
        ConfirmDialog(
            onDismissRequest = { pendingConfirm = null },
            title = pending.label,
            titleIcon = if (pending.isDestructive) Icons.Default.Delete else pending.icon,
            titleIconTint = if (pending.isDestructive) MaterialTheme.colorScheme.error else null,
            text = pending.confirmMessage,
            confirmLabel = confirmLabel,
            confirmIcon = pending.icon,
            isDestructive = pending.isDestructive,
            onConfirm = {
                pending.onClick()
                pendingConfirm = null
                onDismiss()
            },
            onCancel = { pendingConfirm = null },
            cancelLabel = stringResource(Res.string.simt_ui_cancel),
        )
    }
}

@Composable
private fun BulkActionList(
    actions: List<ActionListItem>,
    onActionSelected: (ActionListItem) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        actions.forEachIndexed { index, action ->
            val tint =
                when {
                    !action.enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    action.isDestructive -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .let { if (action.enabled) it.clickable { onActionSelected(action) } else it }
                        .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = action.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = tint,
                )
            }
            if (index != actions.lastIndex) {
                GradientDivider(alpha = 0.25f)
            }
        }
    }
}
