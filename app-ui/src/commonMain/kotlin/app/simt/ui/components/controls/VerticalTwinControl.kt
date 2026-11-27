package app.simt.ui.components.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private val TwinControlWidth = 54.dp
private val TwinControlButtonHeight = 48.dp

/** A compact vertical control with two independently enabled icon actions. */
@Composable
public fun VerticalTwinControl(
    topIcon: ImageVector,
    bottomIcon: ImageVector,
    topContentDescription: String,
    bottomContentDescription: String,
    onTopClick: () -> Unit,
    onBottomClick: () -> Unit,
    modifier: Modifier = Modifier,
    topEnabled: Boolean = true,
    bottomEnabled: Boolean = true,
    accent: Color = MaterialTheme.colorScheme.tertiary,
) {
    val shape = RoundedCornerShape(22.dp)
    val container =
        Brush.verticalGradient(
            colors =
                listOf(
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.95f),
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.88f),
                ),
        )
    val borderColor = accent.copy(alpha = 0.35f)

    Surface(
        modifier = modifier.width(TwinControlWidth),
        shape = shape,
        color = Color.Transparent,
        shadowElevation = 6.dp,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Box(
            modifier =
                Modifier
                    .clip(shape)
                    .background(container),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(3.dp),
            ) {
                TwinControlHalf(
                    enabled = topEnabled,
                    onClick = onTopClick,
                    contentDescription = topContentDescription,
                    imageVector = topIcon,
                    tintActive = accent,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                )
                TwinControlHalf(
                    enabled = bottomEnabled,
                    onClick = onBottomClick,
                    contentDescription = bottomContentDescription,
                    imageVector = bottomIcon,
                    tintActive = accent,
                )
            }
        }
    }
}

@Composable
private fun TwinControlHalf(
    enabled: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    imageVector: ImageVector,
    tintActive: Color,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val iconTint =
        if (enabled) {
            tintActive
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.34f)
        }
    Box(
        modifier =
            Modifier
                .height(TwinControlButtonHeight)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .semantics(mergeDescendants = true) {
                    this.contentDescription = contentDescription
                    this.role = Role.Button
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true),
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp),
        )
    }
}
