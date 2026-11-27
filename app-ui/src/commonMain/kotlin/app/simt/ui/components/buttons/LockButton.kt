package app.simt.ui.components.buttons

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.simt.ui.resources.Res
import app.simt.ui.resources.simt_ui_action_lock
import app.simt.ui.resources.simt_ui_action_unlock
import org.jetbrains.compose.resources.stringResource

@Composable
public fun LockButton(
    isLocked: Boolean,
    onClick: () -> Unit,
    bgColor: Color = MaterialTheme.colorScheme.surface,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isLocked) 1.1f else 1.0f,
        animationSpec = tween(durationMillis = 200),
        label = "scale",
    )
    val backgroundColor by animateColorAsState(
        targetValue =
            if (isLocked) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
            } else {
                bgColor.copy(alpha = 0.15f)
            },
        animationSpec = tween(durationMillis = 300),
        label = "backgroundColor",
    )
    val iconColor by animateColorAsState(
        targetValue =
            if (isLocked) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            },
        animationSpec = tween(durationMillis = 300),
        label = "iconColor",
    )

    Box(
        modifier = modifier.size(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isLocked) {
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .background(
                            brush =
                                Brush.radialGradient(
                                    colors =
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            Color.Transparent,
                                        ),
                                ),
                            shape = CircleShape,
                        ),
            )
        }

        IconButton(
            onClick = onClick,
            modifier =
                Modifier
                    .size(48.dp)
                    .scale(scale)
                    .background(
                        color = backgroundColor,
                        shape = CircleShape,
                    )
                    .border(
                        width = if (isLocked) 2.dp else 1.dp,
                        color =
                            if (isLocked) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            },
                        shape = CircleShape,
                    ),
        ) {
            Icon(
                imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                contentDescription = stringResource(if (isLocked) Res.string.simt_ui_action_unlock else Res.string.simt_ui_action_lock),
                tint = iconColor,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
