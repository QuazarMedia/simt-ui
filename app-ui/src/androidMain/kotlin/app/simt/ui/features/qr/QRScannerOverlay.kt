package app.simt.ui.features.qr

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.simt.ui.resources.Res
import app.simt.ui.resources.simt_ui_qr_scanner_prompt
import org.jetbrains.compose.resources.stringResource

private val AccentCyan = Color(0xFF00E5FF)
private val SuccessGreen = Color(0xFF00E676)

/**
 * Animated scanning overlay for the QR code scanner.
 *
 * Draws a dark mask with a rounded-rect cutout, pulsing corner brackets,
 * a sweeping scan line, and a prompt label below the scan area.
 *
 * @param isScanned When `true`, animations freeze, the accent colour turns green,
 *                  and the scan area scales slightly to indicate success.
 */
@Composable
public fun QRScannerOverlay(isScanned: Boolean) {
    val density = LocalDensity.current

    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")

    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(2500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "scanLine",
    )

    val cornerPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "cornerPulse",
    )

    val borderGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.25f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "borderGlow",
    )

    // Success animations
    val successScale by animateFloatAsState(
        targetValue = if (isScanned) 1.04f else 1f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "successScale",
    )

    val accentColor by animateColorAsState(
        targetValue = if (isScanned) SuccessGreen else AccentCyan,
        animationSpec = tween(200),
        label = "accentColor",
    )

    val bracketArmDp = 26.dp
    val bracketStrokeDp = 3.dp
    val cornerRadiusDp = 14.dp
    val scanAreaFraction = 0.70f
    val verticalOffsetDp = (-32).dp

    val bracketArm = with(density) { bracketArmDp.toPx() }
    val bracketStroke = with(density) { bracketStrokeDp.toPx() }
    val cornerRadius = with(density) { cornerRadiusDp.toPx() }
    val verticalOffset = with(density) { verticalOffsetDp.toPx() }
    val glowHeight = with(density) { 48.dp.toPx() }

    Canvas(
        modifier =
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = successScale
                    scaleY = successScale
                },
    ) {
        val scanSize = size.width * scanAreaFraction
        val scanLeft = (size.width - scanSize) / 2f
        val scanTop = (size.height - scanSize) / 2f + verticalOffset
        val scanRight = scanLeft + scanSize
        val scanBottom = scanTop + scanSize

        // 1. Dark overlay with rounded-rect cutout (EvenOdd fill)
        val overlayPath =
            Path().apply {
                addRect(Rect(0f, 0f, size.width, size.height))
                addRoundRect(RoundRect(scanLeft, scanTop, scanRight, scanBottom, CornerRadius(cornerRadius)))
                fillType = PathFillType.EvenOdd
            }
        drawPath(overlayPath, Color.Black.copy(alpha = 0.65f))

        // 2. Border glow
        drawRoundRect(
            color = accentColor.copy(alpha = borderGlowAlpha),
            topLeft = Offset(scanLeft, scanTop),
            size = Size(scanSize, scanSize),
            cornerRadius = CornerRadius(cornerRadius),
            style = Stroke(width = with(density) { 1.5.dp.toPx() }),
        )

        // 3. Corner brackets
        val effectiveAlpha = if (isScanned) 1f else cornerPulseAlpha
        drawCornerBrackets(
            scanLeft = scanLeft,
            scanTop = scanTop,
            scanRight = scanRight,
            scanBottom = scanBottom,
            armLength = bracketArm,
            strokeWidth = bracketStroke,
            color = accentColor.copy(alpha = effectiveAlpha),
        )

        // 4. Scan line + glow trail (pause when scanned)
        if (!isScanned) {
            val lineY = scanTop + scanSize * scanLineProgress

            // Glow trail above line
            drawRect(
                brush =
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                Color.Transparent,
                                accentColor.copy(alpha = 0.12f),
                                accentColor.copy(alpha = 0.04f),
                                Color.Transparent,
                            ),
                        startY = (lineY - glowHeight).coerceAtLeast(scanTop),
                        endY = lineY,
                    ),
                topLeft = Offset(scanLeft, (lineY - glowHeight).coerceAtLeast(scanTop)),
                size = Size(scanSize, (glowHeight).coerceAtMost(lineY - scanTop)),
            )

            // Scan line
            drawLine(
                brush =
                    Brush.horizontalGradient(
                        colors =
                            listOf(
                                Color.Transparent,
                                accentColor.copy(alpha = 0.7f),
                                accentColor,
                                accentColor.copy(alpha = 0.7f),
                                Color.Transparent,
                            ),
                        startX = scanLeft,
                        endX = scanRight,
                    ),
                start = Offset(scanLeft, lineY),
                end = Offset(scanRight, lineY),
                strokeWidth = with(density) { 2.dp.toPx() },
            )
        }
    }

    // 5. Prompt text
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val promptOffsetY = with(density) { ((density.density * 160f) * scanAreaFraction / 2f + 40.dp.toPx()).toInt() }

        Text(
            text = stringResource(Res.string.simt_ui_qr_scanner_prompt),
            modifier =
                Modifier
                    .offset { IntOffset(0, promptOffsetY) }
                    .padding(horizontal = 40.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
        )
    }
}

// ---------------------------------------------------------------------------
// Corner-bracket drawing helper
// ---------------------------------------------------------------------------

private fun DrawScope.drawCornerBrackets(
    scanLeft: Float,
    scanTop: Float,
    scanRight: Float,
    scanBottom: Float,
    armLength: Float,
    strokeWidth: Float,
    color: Color,
) {
    val half = strokeWidth / 2f

    // Top-left
    drawLine(color, Offset(scanLeft + half, scanTop + half), Offset(scanLeft + armLength, scanTop + half), strokeWidth, StrokeCap.Round)
    drawLine(color, Offset(scanLeft + half, scanTop + half), Offset(scanLeft + half, scanTop + armLength), strokeWidth, StrokeCap.Round)

    // Top-right
    drawLine(color, Offset(scanRight - half, scanTop + half), Offset(scanRight - armLength, scanTop + half), strokeWidth, StrokeCap.Round)
    drawLine(color, Offset(scanRight - half, scanTop + half), Offset(scanRight - half, scanTop + armLength), strokeWidth, StrokeCap.Round)

    // Bottom-left
    drawLine(color, Offset(scanLeft + half, scanBottom - half), Offset(scanLeft + armLength, scanBottom - half), strokeWidth, StrokeCap.Round)
    drawLine(color, Offset(scanLeft + half, scanBottom - half), Offset(scanLeft + half, scanBottom - armLength), strokeWidth, StrokeCap.Round)

    // Bottom-right
    drawLine(color, Offset(scanRight - half, scanBottom - half), Offset(scanRight - armLength, scanBottom - half), strokeWidth, StrokeCap.Round)
    drawLine(color, Offset(scanRight - half, scanBottom - half), Offset(scanRight - half, scanBottom - armLength), strokeWidth, StrokeCap.Round)
}
