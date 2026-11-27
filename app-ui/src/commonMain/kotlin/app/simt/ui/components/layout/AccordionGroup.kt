package app.simt.ui.components.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.simt.ui.components.canopyGlass
import app.simt.ui.effects.LocalUiMotionEnabled
import app.simt.ui.effects.SimtMotion

/** A caller-controlled expandable group with an optional count and tri-state action. */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
public fun AccordionGroup(
    title: String,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    expandLabel: String,
    collapseLabel: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    itemCount: Int? = null,
    triState: ToggleableState? = null,
    onToggleAll: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val motionEnabled = LocalUiMotionEnabled.current
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec =
            if (motionEnabled) {
                tween(durationMillis = SimtMotion.MEDIUM, easing = SimtMotion.TransformEasing)
            } else {
                snap()
            },
        label = "accordion_rotation",
    )
    val disclosureLabel = if (isExpanded) collapseLabel else expandLabel

    Card(
        modifier = modifier.fillMaxWidth().canopyGlass(shape = MaterialTheme.shapes.medium, surfaceAlpha = 0.62f),
        shape = MaterialTheme.shapes.medium,
        colors =
            CardDefaults.cardColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val headerClickModifier =
                    if (onLongPress != null) {
                        Modifier.combinedClickable(
                            role = Role.Button,
                            onClick = { onExpandedChange(!isExpanded) },
                            onLongClick = onLongPress,
                        )
                    } else {
                        Modifier.clickable(role = Role.Button) { onExpandedChange(!isExpanded) }
                    }
                Row(
                    modifier = Modifier.weight(1f).then(headerClickModifier),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (triState != null && onToggleAll != null) {
                        TriStateCheckbox(
                            state = triState,
                            onClick = onToggleAll,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (itemCount != null) {
                                Surface(
                                    modifier = Modifier.size(24.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = itemCount.toString(),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }
                                }
                            }
                        }
                        subtitle?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }

                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .semantics { contentDescription = disclosureLabel }
                            .clickable(role = Role.Button, onClickLabel = disclosureLabel) {
                                onExpandedChange(!isExpanded)
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.rotate(rotationAngle),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (motionEnabled) {
                AnimatedVisibility(
                    visible = isExpanded,
                    enter =
                        expandVertically(
                            animationSpec = tween(durationMillis = SimtMotion.MEDIUM, easing = SimtMotion.EnterEasing),
                            expandFrom = Alignment.Top,
                        ) + fadeIn(animationSpec = tween(durationMillis = SimtMotion.MEDIUM, easing = SimtMotion.EnterEasing)),
                    exit =
                        shrinkVertically(
                            animationSpec = tween(durationMillis = SimtMotion.MEDIUM, easing = SimtMotion.ExitEasing),
                            shrinkTowards = Alignment.Top,
                        ) + fadeOut(animationSpec = tween(durationMillis = SimtMotion.FAST, easing = SimtMotion.ExitEasing)),
                ) {
                    AccordionContent(content)
                }
            } else if (isExpanded) {
                AccordionContent(content)
            }
        }
    }
}

@Composable
private fun AccordionContent(content: @Composable () -> Unit) {
    Column {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 0.5.dp,
        )
        content()
    }
}
