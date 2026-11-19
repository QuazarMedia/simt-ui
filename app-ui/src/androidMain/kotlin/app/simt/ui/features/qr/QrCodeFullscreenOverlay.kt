package app.simt.ui.features.qr

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.simt.ui.components.dialogs.ModalDefaults
import io.github.alexzhirkevich.qrose.options.QrBallShape
import io.github.alexzhirkevich.qrose.options.QrFrameShape
import io.github.alexzhirkevich.qrose.options.QrPixelShape
import io.github.alexzhirkevich.qrose.options.circle
import io.github.alexzhirkevich.qrose.options.roundCorners
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlin.math.min

private const val ANIMATION_DURATION_MS = 300
private const val INITIAL_SCALE = 0.8f

/**
 * Shared full-screen SIMT QR presentation.
 *
 * Download/share controls are optional so short-lived or sensitive QR payloads can reuse the same
 * visual treatment without exposing export actions.
 */
@Composable
public fun QrCodeFullscreenOverlay(
    qrContent: String,
    label: String,
    contentDescription: String,
    onDismiss: () -> Unit,
    downloadLabel: String? = null,
    onDownload: ((Painter) -> Unit)? = null,
    shareLabel: String? = null,
    onShare: ((Painter) -> Unit)? = null,
) {
    val painter = rememberSimtQrCodePainter(qrContent)
    val configuration = LocalConfiguration.current
    val maxOverlayWidth = qrOverlayMaxWidthDp(configuration.screenWidthDp, configuration.screenHeightDp).dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = ModalDefaults.FullScreenDialogProperties,
    ) {
        var animationVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { animationVisible = true }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = animationVisible,
                enter =
                    scaleIn(
                        initialScale = INITIAL_SCALE,
                        animationSpec = tween(ANIMATION_DURATION_MS, easing = FastOutSlowInEasing),
                    ) + fadeIn(animationSpec = tween(ANIMATION_DURATION_MS)),
                exit =
                    scaleOut(
                        targetScale = INITIAL_SCALE,
                        animationSpec = tween(ANIMATION_DURATION_MS, easing = FastOutSlowInEasing),
                    ) + fadeOut(animationSpec = tween(ANIMATION_DURATION_MS)),
            ) {
                Column(
                    modifier =
                        Modifier
                            .widthIn(max = maxOverlayWidth)
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
                            .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painter,
                        contentDescription = contentDescription,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (onDownload != null || onShare != null) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(bottom = 32.dp)
                            .navigationBarsPadding(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                ) {
                    if (onDownload != null && downloadLabel != null) {
                        QrActionButton(
                            icon = Icons.Default.Download,
                            label = downloadLabel,
                            onClick = { onDownload(painter) },
                        )
                    }
                    if (onShare != null && shareLabel != null) {
                        QrActionButton(
                            icon = Icons.Default.Share,
                            label = shareLabel,
                            onClick = { onShare(painter) },
                        )
                    }
                }
            }
        }
    }
}

/** Keeps the square QR surface bounded by the shorter screen edge in every orientation. */
internal fun qrOverlayMaxWidthDp(
    screenWidthDp: Int,
    screenHeightDp: Int,
): Int = min(screenWidthDp, screenHeightDp)

/** The QR shape used by both compact and full-screen SIMT QR surfaces. */
@Composable
public fun rememberSimtQrCodePainter(content: String): Painter =
    rememberQrCodePainter(content) {
        shapes {
            frame = QrFrameShape.roundCorners(corner = .25f)
            ball = QrBallShape.circle()
            darkPixel = QrPixelShape.roundCorners(.5f)
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QrActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
    ) {
        IconButton(
            onClick = onClick,
            modifier =
                Modifier
                    .size(56.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
            )
        }
    }
}
