package app.simt.ui.components.layout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.simt.ui.effects.LongPressTooltipEffect

/** Optional leading action shown on [EditableItemCard] before the edit control. */
public data class EditableItemViewAction(
    val onClick: () -> Unit,
    val contentDescription: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
)

/** A caller-owned editable list item with optional warning, view, edit and delete actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun EditableItemCard(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    description: String? = null,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    isEditing: Boolean,
    isEditEnabled: Boolean,
    isDeleteEnabled: Boolean,
    editLabel: String,
    deleteLabel: String,
    warningMessage: String? = null,
    onView: EditableItemViewAction? = null,
    onStartEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onEditSave: () -> Unit,
    onEditCancel: () -> Unit,
    editContent: (@Composable (onSave: () -> Unit, onCancel: () -> Unit) -> Unit)? = null,
    onTitleClick: (() -> Unit)? = null,
    titleClickLabel: String? = null,
) {
    if (isEditing && editContent != null) {
        editContent(onEditSave, onEditCancel)
        return
    }

    val hasWarning = !warningMessage.isNullOrBlank()
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (hasWarning) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (hasWarning) Icons.Default.WarningAmber else icon,
                        contentDescription = null,
                        tint = if (hasWarning) MaterialTheme.colorScheme.error else iconTint,
                        modifier = Modifier.size(24.dp),
                    )

                    Column(
                        modifier =
                            Modifier.weight(1f).let { base ->
                                val tap = onTitleClick
                                if (tap != null) {
                                    base.clickable(
                                        role = Role.Button,
                                        onClickLabel = titleClickLabel,
                                        onClick = tap,
                                    )
                                } else {
                                    base
                                }
                            },
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (!description.isNullOrBlank()) {
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }
                    }
                }

                onView?.let { action ->
                    LongPressTooltipEffect(
                        tooltipText = action.contentDescription,
                        onClick = { if (action.enabled) action.onClick() },
                        fillMaxHeight = false,
                        contentAlignment = Alignment.Center,
                    ) {
                        FilledTonalIconButton(
                            onClick = action.onClick,
                            enabled = action.enabled,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = action.contentDescription,
                                tint =
                                    if (action.enabled) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    },
                            )
                        }
                    }
                }

                if (onStartEdit != null) {
                    LongPressTooltipEffect(
                        tooltipText = editLabel,
                        onClick = { if (isEditEnabled) onStartEdit() },
                        fillMaxHeight = false,
                        contentAlignment = Alignment.Center,
                    ) {
                        IconButton(
                            onClick = onStartEdit,
                            modifier = Modifier.size(48.dp),
                            enabled = isEditEnabled,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = editLabel,
                                tint =
                                    if (isEditEnabled) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    },
                            )
                        }
                    }
                }

                if (onDelete != null) {
                    LongPressTooltipEffect(
                        tooltipText = deleteLabel,
                        onClick = { if (isDeleteEnabled) onDelete() },
                        fillMaxHeight = false,
                        contentAlignment = Alignment.Center,
                    ) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(48.dp),
                            enabled = isDeleteEnabled,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = deleteLabel,
                                tint =
                                    if (isDeleteEnabled) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.38f)
                                    },
                            )
                        }
                    }
                }
            }

            if (hasWarning) {
                Text(
                    text = warningMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                )
            }
        }
    }
}
