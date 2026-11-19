package app.simt.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import app.simt.ui.components.buttons.CancelLinkButton
import app.simt.ui.components.buttons.SaveButton
import app.simt.ui.effects.ScrollEdgeEffect

/**
 * Generic confirmation dialog, rendered on the Cosmos [SimtDialog] glass panel.
 *
 * - Optional cancel button.
 * - Optional confirm button (styled via [SaveButton]).
 * - Supports title + optional subtitle.
 * - Supports either a simple [text] body or custom [content].
 *
 * This component standardizes spacing, typography, and action styling across the app.
 */
@Composable
public fun ConfirmDialog(
    onDismissRequest: () -> Unit,
    title: String,
    titleIcon: ImageVector? = null,
    titleIconTint: Color? = null,
    subtitle: String? = null,
    text: String? = null,
    content: (@Composable () -> Unit)? = null,
    confirmLabel: String? = null,
    confirmIcon: ImageVector = Icons.Default.Check,
    onConfirm: (() -> Unit)?,
    confirmEnabled: Boolean = true,
    isDestructive: Boolean = false,
    onCancel: (() -> Unit)? = onDismissRequest,
    cancelEnabled: Boolean = true,
    cancelLabel: String? = null,
    cancelIcon: ImageVector = Icons.Default.Close,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    onClose: (() -> Unit)? = null,
    /**
     * When `true` (default), the dialog body is wrapped in a height-capped,
     * vertically-scrollable column so long content remains usable on short
     * screens. Set to `false` for content that manages its own scrolling
     * (e.g. `LazyColumn`, `LazyVerticalGrid`) — a nested scroll container
     * with unbounded height would otherwise crash measurement.
     */
    scrollableContent: Boolean = true,
    modifier: Modifier = Modifier,
    usePlatformDefaultWidth: Boolean = true,
) {
    val confirmColors =
        if (isDestructive) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            )
        } else {
            null
        }

    // Destructive prompts flip the glass border/halo to the error color so the
    // panel itself signals severity before the user reads a single word.
    val accent =
        when {
            isDestructive -> MaterialTheme.colorScheme.error
            titleIconTint != null -> titleIconTint
            else -> MaterialTheme.colorScheme.primary
        }

    SimtDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        variant = SimtDialogVariant.Alert,
        accentColor = accent,
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (titleIcon != null) {
                        // Warm icon orb — same treatment as the command-deck menu rows.
                        Box(
                            modifier =
                                Modifier
                                    .size(32.dp)
                                    .background(accent.copy(alpha = 0.14f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = titleIcon,
                                contentDescription = null,
                                tint = titleIconTint ?: accent,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    if (onClose != null) {
                        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        text = {
            // Cap the body height relative to the viewport and (optionally) make
            // it scrollable so long content (dense settings, cleanup lists, legal
            // text…) doesn't push the action buttons off-screen on short devices.
            // Callers with self-scrolling content (LazyColumn, LazyVerticalGrid,
            // etc.) opt out via [scrollableContent] = false to avoid a nested
            // scroll-container measurement crash.
            val maxBodyHeight = LocalConfiguration.current.screenHeightDp.dp * 0.6f
            val topPadding = if (subtitle.isNullOrBlank()) 4.dp else 0.dp
            val body: @Composable () -> Unit = {
                when {
                    content != null -> content()
                    !text.isNullOrBlank() -> {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            if (scrollableContent) {
                val bodyScrollState = rememberScrollState()
                ScrollEdgeEffect(
                    state = bodyScrollState,
                    orientation = Orientation.Vertical,
                    modifier =
                        Modifier
                            .padding(top = topPadding)
                            .heightIn(max = maxBodyHeight),
                ) {
                    Column(modifier = Modifier.verticalScroll(bodyScrollState)) {
                        body()
                    }
                }
            } else {
                Column(
                    modifier =
                        Modifier
                            .padding(top = topPadding)
                            .heightIn(max = maxBodyHeight),
                ) {
                    body()
                }
            }
        },
        confirmButton = {
            if (onConfirm != null) {
                SaveButton(
                    onClick = onConfirm,
                    label = confirmLabel,
                    icon = confirmIcon,
                    enabled = confirmEnabled,
                    colors = confirmColors,
                )
            }
        },
        dismissButton =
            onCancel?.let { cancel ->
                {
                    CancelLinkButton(
                        onClick = cancel,
                        label = cancelLabel,
                        icon = cancelIcon,
                        enabled = cancelEnabled,
                    )
                }
            },
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = dismissOnClickOutside,
        usePlatformDefaultWidth = usePlatformDefaultWidth,
    )
}
