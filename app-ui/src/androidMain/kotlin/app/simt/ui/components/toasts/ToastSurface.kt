package app.simt.ui.components.toasts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.simt.ui.components.canopyGlass

/** How long a toast stays on screen before it auto-dismisses. */
internal const val TOAST_DURATION_MILLIS = 3000L

/** Accent color for a success toast (green). */
internal val ToastSuccessColor = Color(0xFF4CAF50)

/** Accent color for an error toast (red). */
internal val ToastErrorColor = Color(0xFFEF4444)

/** Accent color for an informational toast (blue). */
internal val ToastInfoColor = Color(0xFF2196F3)

/**
 * The bare canopy-glass toast visual — translucent warm surface with [accentColor] as the border +
 * icon tint. Carries no animation or auto-dismiss timer; the app-wide [GlobalToastHost] owns both.
 */
@Composable
public fun ToastSurface(
    message: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .canopyGlass(
                    shape = MaterialTheme.shapes.medium,
                    surfaceAlpha = 0.94f,
                    borderAlpha = 0.55f,
                    accentColor = accentColor,
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurface,
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
