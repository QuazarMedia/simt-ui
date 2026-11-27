package app.simt.ui.components.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun OverlayIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    tint: Color = Color.White,
    backgroundColor: Color = Color(0x33000000),
    borderColor: Color = Color(0x40FFFFFF),
) {
    if (contentDescription != null) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text(contentDescription) } },
            state = rememberTooltipState(),
        ) {
            OverlayIconButtonContent(
                icon = icon,
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                contentDescription = contentDescription,
                tint = tint,
                backgroundColor = backgroundColor,
                borderColor = borderColor,
            )
        }
    } else {
        OverlayIconButtonContent(
            icon = icon,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            contentDescription = null,
            tint = tint,
            backgroundColor = backgroundColor,
            borderColor = borderColor,
        )
    }
}

@Composable
private fun OverlayIconButtonContent(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    contentDescription: String?,
    tint: Color,
    backgroundColor: Color,
    borderColor: Color,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier
                .background(backgroundColor, shape = CircleShape)
                .border(1.dp, borderColor, CircleShape),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
        )
    }
}
