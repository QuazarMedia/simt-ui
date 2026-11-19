package app.simt.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Generic informational dialog (Layer-2 preset): a title (with optional icon orb), an optional
 * body message, and optional rich [content] — presented on the scrollable [ContentDialog] card
 * with its standard close (X) affordance.
 *
 * Use this for "what is this?" explainers and read-only detail panes instead of hand-rolling a
 * `ContentDialog { Column { Text(title); … } }` each time. For yes/no decisions use [ConfirmDialog];
 * for a single text field use [PromptDialog].
 *
 * @param title Dialog title.
 * @param onDismiss Called on close (X), scrim tap, and back press.
 * @param titleIcon Optional icon shown in the title orb.
 * @param message Optional simple body paragraph shown under the title.
 * @param content Optional rich body appended under [message].
 */
@Composable
public fun InfoDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    titleIcon: ImageVector? = null,
    message: String? = null,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    ContentDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = dismissOnClickOutside,
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (titleIcon != null) {
                    Box(
                        modifier =
                            Modifier
                                .size(32.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                    CircleShape,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = titleIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (!message.isNullOrBlank()) {
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (content != null) {
                Spacer(modifier = Modifier.size(12.dp))
                content()
            }
        }
    }
}
