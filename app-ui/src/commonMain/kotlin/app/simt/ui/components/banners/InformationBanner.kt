package app.simt.ui.components.banners

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.simt.ui.resources.Res
import app.simt.ui.resources.simt_ui_close
import org.jetbrains.compose.resources.stringResource

/**
 * A stylized information banner used for permissions, warnings, or other important messages.
 * Matches the style of permission cards in Launch Gate dialogs.
 *
 * @param text The message to display
 * @param title Optional heading shown above [text]
 * @param icon The icon to show on the left
 * @param tintColor The primary color for the icon and derived border
 * @param backgroundColor The background color of the banner
 * @param onClose Optional callback for a compact close action at the top end
 * @param modifier Modifier for the banner
 */
@Composable
public fun InformationBanner(
    text: String,
    title: String? = null,
    icon: ImageVector = Icons.Default.Info,
    tintColor: Color,
    backgroundColor: Color,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, tintColor.copy(alpha = 0.2f)),
    ) {
        Box {
            Row(
                modifier = Modifier.padding(start = 12.dp, top = 12.dp, end = if (onClose != null) 40.dp else 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(20.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    if (title != null) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 16.sp,
                    )
                }
            }
            if (onClose != null) {
                IconButton(
                    onClick = onClose,
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 4.dp)
                            .size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(Res.string.simt_ui_close),
                        tint = tintColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}
