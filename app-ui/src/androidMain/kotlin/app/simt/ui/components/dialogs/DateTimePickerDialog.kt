package app.simt.ui.components.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.simt.ui.resources.Res
import app.simt.ui.resources.simt_ui_back
import app.simt.ui.resources.simt_ui_cancel
import app.simt.ui.resources.simt_ui_next
import app.simt.ui.resources.simt_ui_ok
import org.jetbrains.compose.resources.stringResource
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

private enum class DateTimePickerStage { DATE, TIME }

/**
 * A reusable date + time picker hosted in a SINGLE dialog window. The dialog shows a Material date
 * picker first, then a time picker; only the inner content swaps between the two steps, so the
 * dialog stays open continuously until both the date *and* time have been chosen. The two are then
 * combined in [initialDateTime]'s zone and delivered to [onConfirm].
 *
 * Using a single window (rather than two chained dialogs) keeps the surface stable — no flicker
 * between steps — and lets the caller treat "the picker is open" as one boolean.
 *
 * @param initialDateTime The instant the pickers open on, and whose zone the confirmed result uses.
 * @param onDismiss       Called when the dialog is dismissed without confirming.
 * @param onConfirm       Called with the combined date + time when the user confirms.
 * @param yearRange       Selectable year range; years/days outside it are disabled. Defaults to the
 *                        Material date-picker range.
 * @param is24Hour        Whether the time picker uses 24-hour format.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun DateTimePickerDialog(
    initialDateTime: ZonedDateTime,
    onDismiss: () -> Unit,
    onConfirm: (ZonedDateTime) -> Unit,
    yearRange: IntRange = DatePickerDefaults.YearRange,
    is24Hour: Boolean = true,
) {
    val zone = initialDateTime.zone
    var stage by remember { mutableStateOf(DateTimePickerStage.DATE) }

    // The date picker tracks days as UTC-midnight millis, independent of the display zone.
    val initialMillis =
        remember(initialDateTime) {
            initialDateTime.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
    val selectableDates =
        remember(yearRange) {
            object : SelectableDates {
                override fun isSelectableYear(year: Int): Boolean = year in yearRange

                override fun isSelectableDate(utcTimeMillis: Long): Boolean = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).year in yearRange
            }
        }
    val dateState =
        rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            initialDisplayedMonthMillis = initialMillis,
            yearRange = yearRange,
            selectableDates = selectableDates,
        )
    val timeState =
        rememberTimePickerState(
            initialHour = initialDateTime.hour,
            initialMinute = initialDateTime.minute,
            is24Hour = is24Hour,
        )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    when (stage) {
                        DateTimePickerStage.DATE -> stage = DateTimePickerStage.TIME
                        DateTimePickerStage.TIME -> {
                            val dateMillis = dateState.selectedDateMillis ?: initialMillis
                            val localDate = Instant.ofEpochMilli(dateMillis).atZone(ZoneOffset.UTC).toLocalDate()
                            onConfirm(ZonedDateTime.of(localDate, LocalTime.of(timeState.hour, timeState.minute), zone))
                        }
                    }
                },
            ) {
                Text(stringResource(if (stage == DateTimePickerStage.DATE) Res.string.simt_ui_next else Res.string.simt_ui_ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { if (stage == DateTimePickerStage.TIME) stage = DateTimePickerStage.DATE else onDismiss() },
            ) {
                Text(stringResource(if (stage == DateTimePickerStage.TIME) Res.string.simt_ui_back else Res.string.simt_ui_cancel))
            }
        },
    ) {
        when (stage) {
            DateTimePickerStage.DATE -> DatePicker(state = dateState)
            DateTimePickerStage.TIME ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TimePicker(state = timeState)
                }
        }
    }
}
