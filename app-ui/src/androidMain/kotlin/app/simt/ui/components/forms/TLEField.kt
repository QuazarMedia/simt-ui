package app.simt.ui.components.forms

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.simt.ui.resources.Res
import app.simt.ui.resources.simt_ui_cd_clear
import app.simt.ui.resources.simt_ui_cd_paste
import app.simt.ui.resources.simt_ui_tle_epoch_status_fmt
import app.simt.ui.resources.simt_ui_tle_epoch_unknown
import app.simt.ui.resources.simt_ui_tle_error_checksum_format
import app.simt.ui.resources.simt_ui_tle_error_checksum_invalid
import app.simt.ui.resources.simt_ui_tle_error_format
import app.simt.ui.resources.simt_ui_tle_error_line_count
import app.simt.ui.resources.simt_ui_tle_error_required
import app.simt.ui.resources.simt_ui_tle_hint_typing
import app.simt.ui.resources.simt_ui_tle_label
import app.simt.ui.resources.simt_ui_tle_ok
import app.simt.ui.resources.simt_ui_tle_placeholder
import org.jetbrains.compose.resources.stringResource
import java.time.DateTimeException
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.floor

public enum class TleValidationError {
    REQUIRED,
    LINE_COUNT,
    FORMAT,
    CHECKSUM_FORMAT,
    CHECKSUM_INVALID,
}

public data class TleValidation(
    val isValid: Boolean,
    val error: TleValidationError? = null,
    val epoch: ZonedDateTime? = null,
    val epochAgeDays: Long? = null,
    val epochFreshness: EpochFreshness = EpochFreshness.UNKNOWN,
)

public enum class EpochFreshness {
    FRESH,
    STALE,
    OLD,
    UNKNOWN,
}

@Composable
public fun TLEField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValidationChanged: ((TleValidation) -> Unit)? = null,
    now: ZonedDateTime? = null,
    maxLength: Int = 1000,
) {
    var isFocused by remember { mutableStateOf(false) }
    var hasValidatedOnce by rememberSaveable { mutableStateOf(false) }
    var draftValue by remember { mutableStateOf(value) }
    var lastValidation by remember { mutableStateOf<TleValidation?>(null) }
    val validationNow = remember(now) { now ?: ZonedDateTime.now(ZoneOffset.UTC) }

    LaunchedEffect(value) {
        if (!isFocused && draftValue != value) {
            draftValue = value
        }
    }

    LaunchedEffect(value, validationNow, isFocused, hasValidatedOnce) {
        if (!isFocused) {
            if (!hasValidatedOnce && value.isNotBlank()) {
                hasValidatedOnce = true
            }
            lastValidation = if (hasValidatedOnce) validateTleText(value, validationNow) else null
        }
    }

    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    val supporting: @Composable (() -> Unit) = {
        val validation = lastValidation
        val hasError = validation?.error != null

        val msg: String? =
            when {
                hasError -> tleErrorMessage(validation.error)
                isFocused -> stringResource(Res.string.simt_ui_tle_hint_typing)
                validation?.epochAgeDays != null -> {
                    val formatted = validation.epoch?.withZoneSameInstant(ZoneOffset.UTC)
                    val epochStr = formatted?.let(DateTimeFormatter.ISO_LOCAL_DATE::format)
                    if (epochStr != null) {
                        stringResource(
                            Res.string.simt_ui_tle_epoch_status_fmt,
                            epochStr,
                            validation.epochAgeDays,
                        )
                    } else {
                        stringResource(Res.string.simt_ui_tle_epoch_unknown)
                    }
                }
                draftValue.isNotBlank() -> stringResource(Res.string.simt_ui_tle_ok)
                else -> null
            }

        val color =
            when {
                hasError -> MaterialTheme.colorScheme.error
                isFocused -> MaterialTheme.colorScheme.onSurfaceVariant
                validation?.epochFreshness == EpochFreshness.FRESH -> Color(0xFF2E7D32)
                validation?.epochFreshness == EpochFreshness.STALE -> Color(0xFFF9A825)
                validation?.epochFreshness == EpochFreshness.OLD -> Color(0xFFC62828)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

        if (msg != null) {
            Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall,
                color = color,
            )
        }
    }

    OutlinedTextField(
        value = draftValue,
        onValueChange = { draftValue = it },
        modifier =
            modifier
                .fillMaxWidth()
                .onFocusChanged { state ->
                    val nowFocused = state.isFocused
                    val wasFocused = isFocused
                    isFocused = nowFocused

                    if (wasFocused && !nowFocused) {
                        hasValidatedOnce = true
                        val sanitized = sanitizeTleText(draftValue, maxLength)
                        draftValue = sanitized
                        onValueChange(sanitized)
                        val validation = validateTleText(sanitized, validationNow)
                        lastValidation = validation
                        onValidationChanged?.invoke(validation)
                    }
                },
        enabled = enabled,
        label = { Text(stringResource(Res.string.simt_ui_tle_label)) },
        placeholder = { Text(stringResource(Res.string.simt_ui_tle_placeholder)) },
        minLines = 2,
        maxLines = 3,
        isError = hasValidatedOnce && (lastValidation?.error != null),
        textStyle =
            TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            ),
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii,
                autoCorrectEnabled = false,
            ),
        trailingIcon = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(end = 2.dp),
            ) {
                IconButton(
                    onClick = {
                        val clip = clipboardManager.primaryClip
                        val text = clip?.getItemAt(0)?.coerceToText(context)?.toString()
                        if (!text.isNullOrBlank()) {
                            hasValidatedOnce = true
                            val sanitized = sanitizeTleText(text, maxLength)
                            draftValue = sanitized
                            onValueChange(sanitized)
                            val validation = validateTleText(sanitized, validationNow)
                            lastValidation = validation
                            onValidationChanged?.invoke(validation)
                        }
                    },
                    enabled = enabled,
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = stringResource(Res.string.simt_ui_cd_paste),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                IconButton(
                    onClick = {
                        lastValidation = null
                        onValueChange("")
                        draftValue = ""
                    },
                    enabled = enabled && draftValue.isNotEmpty(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(Res.string.simt_ui_cd_clear),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        supportingText = supporting,
    )
}

@Composable
private fun tleErrorMessage(error: TleValidationError?): String? =
    when (error) {
        TleValidationError.REQUIRED -> stringResource(Res.string.simt_ui_tle_error_required)
        TleValidationError.LINE_COUNT -> stringResource(Res.string.simt_ui_tle_error_line_count)
        TleValidationError.FORMAT -> stringResource(Res.string.simt_ui_tle_error_format)
        TleValidationError.CHECKSUM_FORMAT -> stringResource(Res.string.simt_ui_tle_error_checksum_format)
        TleValidationError.CHECKSUM_INVALID -> stringResource(Res.string.simt_ui_tle_error_checksum_invalid)
        null -> null
    }

public fun sanitizeTleText(
    input: String,
    maxLength: Int = 1000,
): String {
    val normalized = input.replace("\r\n", "\n").replace("\r", "\n").trimEnd()
    val limited = normalized.take(maxLength)
    val lines = limited.split("\n").take(3)
    return lines.joinToString("\n")
}

public fun validateTleText(
    text: String,
    now: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),
): TleValidation {
    val trimmed = text.trim()
    if (trimmed.isBlank()) {
        return TleValidation(isValid = false, error = TleValidationError.REQUIRED)
    }

    val lines = trimmed.split("\n").map { it.trimEnd() }.filter { it.isNotBlank() }
    if (lines.size < 2 || lines.size > 3) {
        return TleValidation(isValid = false, error = TleValidationError.LINE_COUNT)
    }

    val (line1, line2) =
        if (lines.size == 2) {
            lines[0] to lines[1]
        } else {
            lines[1] to lines[2]
        }

    if (!line1.startsWith("1 ") || !line2.startsWith("2 ")) {
        return TleValidation(isValid = false, error = TleValidationError.FORMAT)
    }

    val checksum1 = computeTleChecksum(line1)
    val checksum2 = computeTleChecksum(line2)
    if (checksum1 == null || checksum2 == null) {
        return TleValidation(isValid = false, error = TleValidationError.CHECKSUM_FORMAT)
    }
    if (!checksum1.isValid || !checksum2.isValid) {
        return TleValidation(isValid = false, error = TleValidationError.CHECKSUM_INVALID)
    }

    val epoch = parseTleEpochUtc(line1)
    val ageDays = epoch?.let { Duration.between(it, now.withZoneSameInstant(ZoneOffset.UTC)).toDays() }
    val freshness =
        when {
            ageDays == null -> EpochFreshness.UNKNOWN
            ageDays < 7 -> EpochFreshness.FRESH
            ageDays < 30 -> EpochFreshness.STALE
            else -> EpochFreshness.OLD
        }

    return TleValidation(
        isValid = true,
        epoch = epoch,
        epochAgeDays = ageDays,
        epochFreshness = freshness,
    )
}

private data class ChecksumResult(val isValid: Boolean)

private fun computeTleChecksum(line: String): ChecksumResult? {
    val last = line.lastOrNull() ?: return null
    val expected = last.digitToIntOrNull() ?: return null

    var sum = 0
    for (i in 0 until line.length - 1) {
        val c = line[i]
        when {
            c.isDigit() -> sum += (c - '0')
            c == '-' -> sum += 1
        }
    }
    val actual = sum % 10
    return ChecksumResult(isValid = actual == expected)
}

private fun parseTleEpochUtc(line1: String): ZonedDateTime? {
    if (line1.length < 32) return null

    val yy = line1.substring(18, 20).trim().toIntOrNull() ?: return null
    val dayStr = line1.substring(20, 32).trim()
    val day = dayStr.toDoubleOrNull() ?: return null

    val year = if (yy >= 57) 1900 + yy else 2000 + yy
    val dayInt = floor(day).toInt()
    if (dayInt <= 0) return null

    val fraction = day - dayInt.toDouble()
    val seconds = (fraction * 86400.0).toLong().coerceAtLeast(0)

    return try {
        LocalDate.ofYearDay(year, dayInt).atStartOfDay().plusSeconds(seconds).atZone(ZoneOffset.UTC)
    } catch (_: DateTimeException) {
        null
    }
}
