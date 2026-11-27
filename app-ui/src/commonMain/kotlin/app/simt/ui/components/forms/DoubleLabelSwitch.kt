package app.simt.ui.components.forms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
public fun DoubleLabelSwitch(
    state1Label: String,
    state2Label: String,
    currentState: Int,
    onStateChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    state1SupportingText: String? = null,
    state2SupportingText: String? = null,
) {
    require(currentState == 1 || currentState == 2) { "currentState must be 1 or 2" }

    val density = LocalDensity.current
    val isState1 = currentState == 1
    val activeSupportingText = if (isState1) state1SupportingText else state2SupportingText
    val disabledAlpha = if (enabled) 1f else 0.5f
    val switchPosition by animateFloatAsState(
        targetValue = if (isState1) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "switch_position",
    )
    val state1InactiveOpacity by animateFloatAsState(
        targetValue = if (isState1) 0f else 0.5f,
        animationSpec = tween(durationMillis = 300),
        label = "state1_inactive_opacity",
    )
    val state2InactiveOpacity by animateFloatAsState(
        targetValue = if (isState1) 0.5f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "state2_inactive_opacity",
    )
    val state1ActiveScale by animateFloatAsState(
        targetValue = if (isState1) 1.1f else 0.9f,
        animationSpec = tween(durationMillis = 300),
        label = "state1_active_scale",
    )
    val state2ActiveScale by animateFloatAsState(
        targetValue = if (isState1) 0.9f else 1.1f,
        animationSpec = tween(durationMillis = 300),
        label = "state2_active_scale",
    )
    val state1ActiveAlpha by animateFloatAsState(
        targetValue = if (isState1) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "state1_active_alpha",
    )
    val state2ActiveAlpha by animateFloatAsState(
        targetValue = if (isState1) 0f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "state2_active_alpha",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .alpha(disabledAlpha)
                    .background(
                        brush =
                            Brush.horizontalGradient(
                                colors =
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    ),
                            ),
                    )
                    .then(
                        if (enabled) {
                            Modifier.clickable {
                                onStateChange(if (isState1) 2 else 1)
                            }
                        } else {
                            Modifier
                        },
                    )
                    .padding(4.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            brush =
                                Brush.horizontalGradient(
                                    colors =
                                        listOf(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                        ),
                                ),
                        ),
            )

            Text(
                text = state2Label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = state2InactiveOpacity),
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp),
            )

            Text(
                text = state1Label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = state1InactiveOpacity),
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
            )

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val trackWidth = with(density) { maxWidth.toPx() }
                val trackHeight = with(density) { 48.dp.toPx() }
                val padding = with(density) { 2.dp.toPx() }
                val thumbWidthPx = maxOf(trackWidth * 0.6f, trackHeight)
                val thumbHeightPx = trackHeight
                val thumbWidthDp = with(density) { thumbWidthPx.toDp() }
                val thumbHeightDp = with(density) { thumbHeightPx.toDp() }
                val maxTravel = trackWidth - thumbWidthPx - (padding * 2)
                val thumbOffsetX = switchPosition * maxTravel + padding
                val cornerRadius = thumbHeightDp / 2

                Box(
                    modifier =
                        Modifier
                            .offset(x = with(density) { thumbOffsetX.toDp() })
                            .size(width = thumbWidthDp, height = thumbHeightDp)
                            .clip(RoundedCornerShape(cornerRadius))
                            .background(
                                brush =
                                    Brush.linearGradient(
                                        colors =
                                            listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                            ),
                                    ),
                            ),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(width = thumbWidthDp * 0.9f, height = thumbHeightDp * 0.75f)
                                .align(Alignment.Center)
                                .clip(RoundedCornerShape(cornerRadius * 0.75f))
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                ),
                    )

                    Text(
                        text = state1Label,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.align(Alignment.Center).alpha(state1ActiveAlpha).scale(state1ActiveScale),
                    )

                    Text(
                        text = state2Label,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.align(Alignment.Center).alpha(state2ActiveAlpha).scale(state2ActiveScale),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !activeSupportingText.isNullOrEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Text(
                text = activeSupportingText.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
