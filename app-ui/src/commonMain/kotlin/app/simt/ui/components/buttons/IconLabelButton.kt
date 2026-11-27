package app.simt.ui.components.buttons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
public fun IconLabelButton(
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    color: Color,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 12.dp,
    backgroundColor: Color? = null,
    minWidth: Dp = 64.dp,
    compact: Boolean = false,
) {
    val contentColor =
        if (enabled) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val effectiveHorizontalPadding =
        if (compact) (horizontalPadding - 4.dp).coerceAtLeast(0.dp) else horizontalPadding
    val effectiveMinWidth = if (compact) (minWidth - 8.dp).coerceAtLeast(0.dp) else minWidth
    val iconSize = if (compact) 20.dp else 24.dp
    val verticalPadding = if (compact) 6.dp else 8.dp
    val labelFontSize = if (compact) 9.sp else 10.sp

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.widthIn(min = effectiveMinWidth),
        color = backgroundColor ?: Color.Transparent,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = effectiveHorizontalPadding, vertical = verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = contentColor,
            )
            Text(
                text = label.uppercase(),
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = labelFontSize,
                        letterSpacing = 0.5.sp,
                    ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}
