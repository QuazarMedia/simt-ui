package app.shared.ui.effects

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
public fun LongPressTooltipEffect(
    tooltipText: String,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    fillMaxHeight: Boolean = true,
    contentAlignment: Alignment = Alignment.TopStart,
    position: TooltipAnchorPosition = TooltipAnchorPosition.Above,
    content: @Composable BoxScope.() -> Unit,
) {
    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(position, 2.dp),
        tooltip = { PlainTooltip { Text(tooltipText) } },
        state = tooltipState,
        modifier = modifier,
        enableUserInput = false,
    ) {
        Box(
            modifier =
                Modifier
                    .then(if (fillMaxHeight) Modifier.fillMaxHeight() else Modifier)
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = { scope.launch { tooltipState.show() } },
                    )
                    .then(contentModifier),
            contentAlignment = contentAlignment,
            content = content,
        )
    }
}
