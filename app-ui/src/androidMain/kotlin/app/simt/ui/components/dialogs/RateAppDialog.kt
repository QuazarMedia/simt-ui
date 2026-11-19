package app.simt.ui.components.dialogs

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.simt.ui.resources.Res
import app.simt.ui.resources.simt_ui_rate_app_feedback_message
import app.simt.ui.resources.simt_ui_rate_app_in_love_message
import app.simt.ui.resources.simt_ui_rate_app_message
import app.simt.ui.resources.simt_ui_rate_app_not_now
import app.simt.ui.resources.simt_ui_rate_app_submit
import app.simt.ui.resources.simt_ui_rate_app_thank_you
import app.simt.ui.resources.simt_ui_rate_app_thank_you_in_love
import app.simt.ui.resources.simt_ui_rate_app_title
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

internal const val RATE_SCORE_ANGRY = 1
internal const val RATE_SCORE_SMIRK = 3
internal const val RATE_SCORE_HAPPY = 4
internal const val RATE_SCORE_IN_LOVE = 5

internal fun ratingOptionsForDialog(): List<RatingOption> =
    listOf(
        RatingOption(score = RATE_SCORE_ANGRY, emoji = "😠"),
        RatingOption(score = RATE_SCORE_SMIRK, emoji = "🙂"),
        RatingOption(score = RATE_SCORE_HAPPY, emoji = "😄"),
        RatingOption(score = RATE_SCORE_IN_LOVE, emoji = "😍"),
    )

internal fun canSubmitRating(selectedScore: Int): Boolean = selectedScore > 0

internal fun isInLoveRating(selectedScore: Int): Boolean = selectedScore == RATE_SCORE_IN_LOVE

internal fun shouldDismissAfterRating(selectedScore: Int): Boolean = !isInLoveRating(selectedScore)

@Composable
public fun RateAppDialog(
    onDismiss: () -> Unit,
    onRate: (Int) -> Unit,
) {
    var selectedScore by remember { mutableIntStateOf(0) }
    var showThankYou by remember { mutableStateOf(false) }
    val ratingOptions = remember { ratingOptionsForDialog() }

    ContentDialog(
        onDismissRequest = onDismiss,
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!showThankYou) {
                Text(
                    text = stringResource(Res.string.simt_ui_rate_app_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(Res.string.simt_ui_rate_app_message),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    ratingOptions.forEach { option ->
                        val isSelected = selectedScore == option.score
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.06f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "emotion_scale_${option.score}",
                        )

                        Column(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                        },
                                    )
                                    .clickable { selectedScore = option.score }
                                    .padding(horizontal = 8.dp, vertical = 12.dp)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = option.emoji,
                                style = MaterialTheme.typography.headlineMedium,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (canSubmitRating(selectedScore)) {
                            showThankYou = true
                        }
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        ),
                    enabled = canSubmitRating(selectedScore),
                ) {
                    Text(
                        text = stringResource(Res.string.simt_ui_rate_app_submit),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.simt_ui_rate_app_not_now),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            } else {
                val isInLove = isInLoveRating(selectedScore)
                val selectedEmoji = ratingOptions.firstOrNull { it.score == selectedScore }?.emoji.orEmpty()

                Text(
                    text = selectedEmoji,
                    style = MaterialTheme.typography.displaySmall,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text =
                        if (isInLove) {
                            stringResource(Res.string.simt_ui_rate_app_thank_you_in_love)
                        } else {
                            stringResource(Res.string.simt_ui_rate_app_thank_you)
                        },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text =
                        if (isInLove) {
                            stringResource(Res.string.simt_ui_rate_app_in_love_message)
                        } else {
                            stringResource(Res.string.simt_ui_rate_app_feedback_message)
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                )

                LaunchedEffect(Unit) {
                    if (!shouldDismissAfterRating(selectedScore)) {
                        // Brief delay for user to read the message, then trigger the review flow.
                        // Dialog stays open; the caller dismisses it after the review completes.
                        delay(1500)
                        onRate(selectedScore)
                    } else {
                        delay(1000)
                        onRate(selectedScore)
                        onDismiss()
                    }
                }
            }
        }
    }
}

internal data class RatingOption(
    val score: Int,
    val emoji: String,
)
