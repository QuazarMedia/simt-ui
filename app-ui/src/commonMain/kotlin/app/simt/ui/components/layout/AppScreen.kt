package app.simt.ui.components.layout

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.simt.ui.components.TopOverlayHeader
import app.simt.ui.components.TopOverlayToolToggle
import app.simt.ui.components.buttons.DialogActionButtons
import app.simt.ui.effects.ScrollEdgeEffect

/** A full-screen layout with caller-owned title, background, actions and content. */
@Composable
public fun AppScreen(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    headerIcon: (@Composable () -> Unit)? = null,
    headerTools: List<TopOverlayToolToggle> = emptyList(),
    onSave: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    isSaveEnabled: Boolean = true,
    saveLabel: String? = null,
    cancelLabel: String? = null,
    customActions: (@Composable RowScope.() -> Unit)? = null,
    isScrollable: Boolean = true,
    headerContentCard: (@Composable () -> Unit)? = null,
    background: (@Composable (Modifier) -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val contentScrollState = rememberScrollState()
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        background?.invoke(Modifier.fillMaxSize())
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
        ) {
            TopOverlayHeader(
                title = title,
                subtitle = subtitle,
                onClose = onDismissRequest,
                icon = headerIcon,
                tools = headerTools,
                contentCard = headerContentCard,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            )

            Box(
                modifier =
                    Modifier
                        .weight(1f, fill = true)
                        .fillMaxWidth(),
            ) {
                if (isScrollable) {
                    ScrollEdgeEffect(
                        state = contentScrollState,
                        orientation = Orientation.Vertical,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(contentScrollState)
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 16.dp, bottom = 8.dp),
                        ) {
                            content()
                        }
                    }
                } else {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 16.dp, bottom = 8.dp),
                    ) {
                        content()
                    }
                }
            }

            if (onSave != null || onClose != null || customActions != null) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )

                DialogActionButtons(
                    onCancel = onCancel,
                    onSave = onSave,
                    onClose = onClose,
                    isSaveEnabled = isSaveEnabled,
                    cancelLabel = cancelLabel,
                    saveLabel = saveLabel,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    customActions = customActions,
                )
            }
        }
    }
}
