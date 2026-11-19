package app.simt.ui.components.permissions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.simt.ui.components.dialogs.SimtDialog
import app.simt.ui.resources.Res
import app.simt.ui.resources.simt_ui_requirement_checking
import app.simt.ui.resources.simt_ui_requirement_fix_dismiss
import app.simt.ui.resources.simt_ui_requirement_fix_it
import app.simt.ui.resources.simt_ui_requirement_limited
import app.simt.ui.resources.simt_ui_requirement_missing
import app.simt.ui.resources.simt_ui_requirement_ready
import app.simt.ui.resources.simt_ui_requirement_unknown
import org.jetbrains.compose.resources.stringResource

/**
 * Availability status of a single feature requirement.
 *
 * Presentation-level mirror of the application's own availability model — applications map
 * their domain status onto this enum at the call site so the shared UI never depends on
 * application code.
 */
public enum class RequirementStatus {
    AVAILABLE,
    PARTIAL,
    UNAVAILABLE,
    CHECKING,
    UNKNOWN,
}

/**
 * Represents a requirement for a feature.
 *
 * @property label The display name of the feature
 * @property status The current availability status of the feature
 * @property description Optional detailed description of the feature and its impact
 * @property fixActionLabel Optional custom label for the fix action button (defaults to "Fix it").
 * @property onFix Optional action that resolves an unmet requirement (e.g. install/enable a
 *   dependency). When provided and the requirement is not satisfied, the info dialog surfaces a
 *   "Fix it" confirm button plus a dismiss button instead of a plain acknowledge button.
 */
public data class FeatureRequirement(
    public val label: String,
    public val status: RequirementStatus,
    public val description: String? = null,
    public val fixActionLabel: String? = null,
    public val onFix: (() -> Unit)? = null,
)

/**
 * Card component that displays a feature requirement status.
 * Shows the requirement label and its availability status with color-coded indicators.
 * If a description is provided, clicking the card shows an info dialog. When the requirement is
 * unmet and exposes a [FeatureRequirement.onFix] action, that dialog turns its confirm button
 * into a "Fix it" button (plus a dismiss) so the user can resolve it inline — e.g. install or
 * enable a missing dependency.
 *
 * @param requirement The feature requirement to display
 * @param modifier Modifier to be applied to the card
 */
@Composable
public fun FeatureRequirementCard(
    requirement: FeatureRequirement,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    // A fix is offered only when the feature wired an [onFix] action AND the requirement is
    // actually unmet (skip while still checking or already available).
    val fixAction =
        requirement.onFix?.takeIf {
            requirement.status != RequirementStatus.AVAILABLE &&
                requirement.status != RequirementStatus.CHECKING
        }
    val isCardInteractive = requirement.description != null || fixAction != null

    val statusColor =
        when (requirement.status) {
            RequirementStatus.AVAILABLE -> Color(0xFF4CAF50) // Green
            RequirementStatus.PARTIAL -> Color(0xFFFF9800) // Orange
            RequirementStatus.UNAVAILABLE -> Color(0xFFF44336) // Red
            RequirementStatus.CHECKING -> Color(0xFFFFEB3B) // Yellow
            RequirementStatus.UNKNOWN -> Color(0xFF9E9E9E) // Gray
        }

    val statusLabel =
        when (requirement.status) {
            RequirementStatus.AVAILABLE -> stringResource(Res.string.simt_ui_requirement_ready)
            RequirementStatus.PARTIAL -> stringResource(Res.string.simt_ui_requirement_limited)
            RequirementStatus.UNAVAILABLE -> stringResource(Res.string.simt_ui_requirement_missing)
            RequirementStatus.CHECKING -> stringResource(Res.string.simt_ui_requirement_checking)
            RequirementStatus.UNKNOWN -> stringResource(Res.string.simt_ui_requirement_unknown)
        }

    if (showDialog && isCardInteractive) {
        SimtDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = requirement.label) },
            text = requirement.description?.let { description -> { Text(text = description) } },
            confirmButton = {
                if (fixAction != null) {
                    TextButton(
                        onClick = {
                            showDialog = false
                            fixAction()
                        },
                    ) {
                        Text(
                            requirement.fixActionLabel
                                ?: stringResource(Res.string.simt_ui_requirement_fix_it),
                        )
                    }
                } else {
                    TextButton(onClick = { showDialog = false }) {
                        Text(androidx.compose.ui.res.stringResource(android.R.string.ok))
                    }
                }
            },
            dismissButton =
                if (fixAction != null) {
                    {
                        TextButton(onClick = { showDialog = false }) {
                            Text(stringResource(Res.string.simt_ui_requirement_fix_dismiss))
                        }
                    }
                } else {
                    null
                },
        )
    }

    Surface(
        modifier =
            modifier.clickable(enabled = isCardInteractive) {
                showDialog = true
            },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = requirement.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(8.dp)
                            .background(statusColor, CircleShape),
                )
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                )
            }
        }
    }
}
