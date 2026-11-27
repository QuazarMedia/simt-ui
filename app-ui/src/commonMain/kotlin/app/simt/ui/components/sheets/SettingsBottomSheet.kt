package app.simt.ui.components.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.simt.ui.effects.ScrollEdgeEffect

/** Theme-owned modal sheet for live settings with caller-owned header, accent and content. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun SettingsBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    overline: String? = null,
    titleIcon: ImageVector? = Icons.Default.Settings,
    accentColor: Color = MaterialTheme.colorScheme.secondary,
    skipPartiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)
    val surface = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val backgroundBrush =
        Brush.verticalGradient(
            colors = listOf(surfaceVariant.copy(alpha = 0.96f), surface.copy(alpha = 0.98f)),
        )
    val accentLineBrush =
        Brush.horizontalGradient(
            colors =
                listOf(
                    Color.Transparent,
                    accentColor.copy(alpha = 0.85f),
                    accentColor,
                    accentColor.copy(alpha = 0.85f),
                    Color.Transparent,
                ),
        )
    val sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentColor = onSurface,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        contentWindowInsets = { WindowInsets(0) },
        dragHandle = {
            Box(
                modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(accentColor.copy(alpha = 0.55f)),
                )
            }
        },
        shape = sheetShape,
        modifier = modifier,
    ) {
        SettingsSheetPanel(
            title = title,
            overline = overline,
            titleIcon = titleIcon,
            accentColor = accentColor,
            backgroundBrush = backgroundBrush,
            accentLineBrush = accentLineBrush,
            sheetShape = sheetShape,
            onSurface = onSurface,
            onSurfaceVariant = onSurfaceVariant,
            content = content,
        )
    }
}

@Composable
private fun SettingsSheetPanel(
    title: String,
    overline: String?,
    titleIcon: ImageVector?,
    accentColor: Color,
    backgroundBrush: Brush,
    accentLineBrush: Brush,
    sheetShape: RoundedCornerShape,
    onSurface: Color,
    onSurfaceVariant: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(sheetShape)
                .background(backgroundBrush)
                .topOnlyAccentBorder(
                    color = accentColor.copy(alpha = 0.35f),
                    cornerRadius = 24.dp,
                    strokeWidth = 1.dp,
                ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(1.dp).background(accentLineBrush),
            )
            val bodyScrollState = rememberScrollState()
            ScrollEdgeEffect(
                state = bodyScrollState,
                orientation = Orientation.Vertical,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(bodyScrollState)
                            .padding(horizontal = 24.dp)
                            .padding(top = 18.dp, bottom = 32.dp)
                            .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Header(
                        title = title,
                        overline = overline,
                        titleIcon = titleIcon,
                        accentColor = accentColor,
                        onSurface = onSurface,
                        onSurfaceVariant = onSurfaceVariant,
                    )
                    content()
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun Header(
    title: String,
    overline: String?,
    titleIcon: ImageVector?,
    accentColor: Color,
    onSurface: Color,
    onSurfaceVariant: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (overline != null) {
            Text(
                text = overline,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Medium),
                color = accentColor.copy(alpha = 0.85f),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (titleIcon != null) {
                Box(
                    modifier =
                        Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.12f))
                            .border(
                                width = 1.dp,
                                color = accentColor.copy(alpha = 0.40f),
                                shape = RoundedCornerShape(8.dp),
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = titleIcon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(modifier = Modifier.width(2.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = onSurface,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(1.dp).background(onSurfaceVariant.copy(alpha = 0.12f)),
        )
    }
}

private fun Modifier.topOnlyAccentBorder(
    color: Color,
    cornerRadius: Dp,
    strokeWidth: Dp,
): Modifier =
    this.drawBehind {
        val r = cornerRadius.toPx()
        val sw = strokeWidth.toPx()
        val inset = sw / 2f
        val w = size.width
        val h = size.height
        val path =
            Path().apply {
                moveTo(inset, h)
                lineTo(inset, r)
                arcTo(
                    rect = Rect(offset = Offset(inset, inset), size = Size(r * 2 - sw, r * 2 - sw)),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false,
                )
                lineTo(w - r, inset)
                arcTo(
                    rect = Rect(offset = Offset(w - r * 2 + inset, inset), size = Size(r * 2 - sw, r * 2 - sw)),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false,
                )
                lineTo(w - inset, h)
            }
        drawPath(path = path, color = color, style = Stroke(width = sw))
    }
