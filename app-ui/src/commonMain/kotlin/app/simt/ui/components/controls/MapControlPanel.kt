package app.simt.ui.components.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.simt.ui.effects.LongPressTooltipEffect

private val MapControlButtonSize = 48.dp
private val MapPanelColor = Color(0xCC1A1F3A)

/** A dark overlay panel with caller-owned recenter, zoom and additional actions. */
@Composable
public fun MapControlPanel(
    onRecenter: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    recenterContentDescription: String,
    zoomInContentDescription: String,
    zoomOutContentDescription: String,
    modifier: Modifier = Modifier,
    recenterIcon: ImageVector = Icons.Default.MyLocation,
    zoomInfo: (@Composable () -> Unit)? = null,
    additionalContent: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier =
            modifier
                .background(
                    color = MapPanelColor,
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MapControlButton(
            icon = recenterIcon,
            onClick = onRecenter,
            contentDescription = recenterContentDescription,
        )

        Spacer(modifier = Modifier.height(10.dp))

        MapControlButton(
            icon = Icons.Default.Add,
            onClick = onZoomIn,
            contentDescription = zoomInContentDescription,
        )

        zoomInfo?.invoke()

        MapControlButton(
            icon = Icons.Default.Remove,
            onClick = onZoomOut,
            contentDescription = zoomOutContentDescription,
        )

        additionalContent()
    }
}

/** A 48 dp map-panel action with an accessible long-press tooltip. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun MapControlButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    enabled: Boolean = true,
) {
    val effectiveTint = if (enabled) tint else tint.copy(alpha = 0.4f)
    LongPressTooltipEffect(
        tooltipText = contentDescription,
        onClick = onClick,
        modifier = modifier,
        contentModifier = Modifier.size(MapControlButtonSize),
        fillMaxHeight = false,
        contentAlignment = Alignment.Center,
        enabled = enabled,
        role = Role.Button,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = effectiveTint,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** A small caller-styled label for map-panel value readouts. */
@Composable
public fun MapControlLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White.copy(alpha = 0.7f),
    fontSize: TextUnit = 11.sp,
    fontWeight: FontWeight? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
    )
}
