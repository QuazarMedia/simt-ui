package app.simt.ui.components.forms

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneOffset
import java.time.ZonedDateTime

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TleFieldTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun sanitizeNormalizesLineEndingsAndCapsLinesAndLength() {
        assertEquals(
            "  ISS\nline one\nline two",
            sanitizeTleText("  ISS\r\nline one\rline two\nline three\r\nline four\r\n"),
        )
        assertEquals("abc", sanitizeTleText("abcdef", maxLength = 3))
    }

    @Test
    fun validationReportsStableErrorsWithoutAndroidResourceIds() {
        assertEquals(TleValidationError.REQUIRED, validateTleText("").error)
        assertEquals(TleValidationError.LINE_COUNT, validateTleText("1 25544U").error)
        assertEquals(TleValidationError.FORMAT, validateTleText("not a tle\nstill not a tle").error)
        assertEquals(TleValidationError.CHECKSUM_FORMAT, validateTleText("1 25544U\n2 25544").error)
        assertEquals(
            TleValidationError.CHECKSUM_INVALID,
            validateTleText("${VALID_LINE_1.dropLast(1)}0\n$VALID_LINE_2").error,
        )
    }

    @Test
    fun validationClassifiesEpochFreshness() {
        val fresh = validateTleText(VALID_TLE, ZonedDateTime.of(2024, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC))
        val stale = validateTleText(VALID_TLE, ZonedDateTime.of(2024, 1, 10, 0, 0, 0, 0, ZoneOffset.UTC))
        val old = validateTleText(VALID_TLE, ZonedDateTime.of(2024, 2, 15, 0, 0, 0, 0, ZoneOffset.UTC))

        assertTrue(fresh.isValid)
        assertEquals(EpochFreshness.FRESH, fresh.epochFreshness)
        assertEquals(EpochFreshness.STALE, stale.epochFreshness)
        assertEquals(EpochFreshness.OLD, old.epochFreshness)
    }

    @Test
    fun validationTreatsAnOutOfRangeEpochAsUnknownInsteadOfThrowing() {
        val prefix = VALID_LINE_1.dropLast(1).replaceRange(20, 32, "367.00000000")
        val line1 = prefix + checksumDigit(prefix)
        val validation = validateTleText("$line1\n$VALID_LINE_2")

        assertTrue(validation.isValid)
        assertNull(validation.error)
        assertNull(validation.epoch)
        assertNull(validation.epochAgeDays)
        assertEquals(EpochFreshness.UNKNOWN, validation.epochFreshness)
    }

    @Test
    fun pasteSanitizesPublishesAndClearEmitsAnEmptyValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("TLE", "ISS (ZARYA)\r\n$VALID_LINE_1\r\n$VALID_LINE_2\r\n"))
        val value = mutableStateOf("")
        val changes = mutableListOf<String>()
        var validation: TleValidation? = null
        compose.setContent {
            MaterialTheme {
                TLEField(
                    value = value.value,
                    onValueChange = {
                        changes.add(it)
                        value.value = it
                    },
                    onValidationChanged = { validation = it },
                    now = ZonedDateTime.of(2024, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC),
                )
            }
        }

        compose.onNodeWithContentDescription("Paste").performClick()
        compose.runOnIdle {
            assertEquals("ISS (ZARYA)\n$VALID_LINE_1\n$VALID_LINE_2", value.value)
            assertEquals(1, changes.size)
            assertTrue(validation?.isValid == true)
        }

        compose.onNodeWithContentDescription("Clear text").performClick()
        compose.runOnIdle {
            assertEquals("", value.value)
            assertEquals(2, changes.size)
        }
        compose.onNodeWithContentDescription("Clear text").assertIsNotEnabled()
    }

    private fun checksumDigit(lineWithoutChecksum: String): Int {
        val sum =
            lineWithoutChecksum.sumOf { c ->
                when {
                    c.isDigit() -> c - '0'
                    c == '-' -> 1
                    else -> 0
                }
            }
        return sum % 10
    }

    private companion object {
        const val VALID_LINE_1 =
            "1 25544U 98067A   24001.50000000  .00016717  00000-0  10270-3 0  9009"
        const val VALID_LINE_2 =
            "2 25544  51.6400 208.9163 0006703 300.8744  59.1882 15.49560773999994"
        const val VALID_TLE = "$VALID_LINE_1\n$VALID_LINE_2"
    }
}
