package app.shared.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.shared.ui.effects.LocalUiMotionEnabled
import app.shared.ui.internal.platform.headerWidthFraction
import app.shared.ui.resources.Res
import app.shared.ui.resources.simt_ui_close
import app.shared.ui.utils.fullScreenTopInsets
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

@Immutable
data class TopOverlayToolToggle(
    val isOn: Boolean,
    val iconOn: ImageVector,
    val iconOff: ImageVector? = null,
    val onClick: (isOn: Boolean) -> Unit,
    val contentDescription: String? = null,
    val iconTint: Color? = null,
    val disabled: Boolean = false,
    val containerBrush: Brush? = null,
)

@Composable
fun TopOverlayHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    applyTopInsets: Boolean = true,
    subtitle: String? = null,
    icon: (@Composable () -> Unit)? = null,
    tools: List<TopOverlayToolToggle> = emptyList(),
    toolsModifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    closeContentDescription: String = stringResource(Res.string.simt_ui_close),
    contentCard: (@Composable () -> Unit)? = null,
    onTitleAreaClick: (() -> Unit)? = null,
) {
    val widthFraction = headerWidthFraction()

    Column(
        modifier =
            modifier.fillMaxWidth()
                .then(if (applyTopInsets) Modifier.fullScreenTopInsets() else Modifier),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier.align(Alignment.Center).fillMaxWidth(widthFraction),
                color = containerColor,
                contentColor = contentColor,
                shape = MaterialTheme.shapes.large,
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        if (icon != null) {
                            icon()
                            Spacer(Modifier.width(8.dp))
                        }
                        Column(
                            modifier =
                                if (onTitleAreaClick != null) Modifier.clickable(onClick = onTitleAreaClick) else Modifier,
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (!subtitle.isNullOrBlank()) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = contentColor.copy(alpha = 0.7f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    if (tools.isNotEmpty()) {
                        TopOverlayHeaderTools(tools, contentColor, toolsModifier)
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = closeContentDescription,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
        TopOverlayHeaderContentCard(
            contentCard = contentCard,
            widthFraction = widthFraction,
            containerColor = containerColor,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopOverlayHeaderTools(
    tools: List<TopOverlayToolToggle>,
    contentColor: Color,
    toolsModifier: Modifier,
) {
    Surface(
        modifier = toolsModifier,
        color = contentColor.copy(alpha = 0.10f),
        contentColor = contentColor,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.20f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            tools.forEach { tool ->
                val iconVector = if (tool.isOn) tool.iconOn else tool.iconOff ?: tool.iconOn
                val toolContainerColor =
                    if (!tool.disabled && tool.isOn) MaterialTheme.colorScheme.primary else Color.Transparent
                val toolContentColor = if (tool.isOn) MaterialTheme.colorScheme.onPrimary else contentColor
                val toolBorder =
                    if (tool.isOn && !tool.disabled) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f))
                    } else {
                        null
                    }
                val effectiveTint =
                    when {
                        tool.disabled -> contentColor.copy(alpha = 0.3f)
                        tool.iconTint != null -> tool.iconTint
                        else -> toolContentColor
                    }
                val iconButton = @Composable {
                    IconButton(
                        onClick = { tool.onClick(tool.isOn) },
                        enabled = !tool.disabled,
                        modifier = Modifier.size(48.dp).semantics { selected = tool.isOn },
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = toolContentColor,
                            ),
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = tool.contentDescription,
                            tint = effectiveTint,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                val surfaceContent = @Composable {
                    if (tool.contentDescription != null) {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below, 2.dp),
                            tooltip = { PlainTooltip { Text(tool.contentDescription) } },
                            state = rememberTooltipState(),
                            enableUserInput = !tool.disabled,
                        ) { iconButton() }
                    } else {
                        iconButton()
                    }
                }
                if (tool.containerBrush != null && !tool.disabled) {
                    Box(
                        modifier = Modifier.size(48.dp).background(tool.containerBrush, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { surfaceContent() }
                } else {
                    Surface(
                        color = toolContainerColor,
                        contentColor = toolContentColor,
                        shape = CircleShape,
                        border = toolBorder,
                    ) { surfaceContent() }
                }
            }
        }
    }
    Spacer(Modifier.width(4.dp))
}

@Composable
private fun TopOverlayHeaderContentCard(
    contentCard: (@Composable () -> Unit)?,
    widthFraction: Float,
    containerColor: Color,
) {
    var rememberedContentCard by remember { mutableStateOf(contentCard) }
    val motionEnabled = LocalUiMotionEnabled.current
    val duration = if (motionEnabled) 300 else 0
    SideEffect {
        if (contentCard != null) rememberedContentCard = contentCard
    }
    LaunchedEffect(contentCard) {
        if (contentCard == null) {
            delay(duration.toLong())
            rememberedContentCard = null
        }
    }
    val displayedContent = contentCard ?: rememberedContentCard
    AnimatedVisibility(
        visible = contentCard != null,
        enter = expandVertically(animationSpec = tween(duration), expandFrom = Alignment.Top),
        exit = shrinkVertically(animationSpec = tween(duration), shrinkTowards = Alignment.Top),
    ) {
        displayedContent?.let { cardContent ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                Card(
                    modifier = Modifier.fillMaxWidth(widthFraction).padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = containerColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Box(Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)) { cardContent() }
                }
            }
        }
    }
}
