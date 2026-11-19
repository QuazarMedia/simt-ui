package app.simt.ui.components.dialogs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RateAppDialogLogicTest {
    @Test
    fun `rating options expose expected emoticon scores`() {
        val options = ratingOptionsForDialog()

        assertEquals(
            listOf(RATE_SCORE_ANGRY, RATE_SCORE_SMIRK, RATE_SCORE_HAPPY, RATE_SCORE_IN_LOVE),
            options.map { it.score },
        )
        assertEquals(listOf("😠", "🙂", "😄", "😍"), options.map { it.emoji })
    }

    @Test
    fun `submit enabled only when score selected`() {
        assertFalse(canSubmitRating(0))
        assertTrue(canSubmitRating(RATE_SCORE_ANGRY))
        assertTrue(canSubmitRating(RATE_SCORE_SMIRK))
        assertTrue(canSubmitRating(RATE_SCORE_HAPPY))
        assertTrue(canSubmitRating(RATE_SCORE_IN_LOVE))
    }

    @Test
    fun `in-love score maps to play review path semantics`() {
        assertFalse(isInLoveRating(RATE_SCORE_ANGRY))
        assertFalse(isInLoveRating(RATE_SCORE_SMIRK))
        assertFalse(isInLoveRating(RATE_SCORE_HAPPY))
        assertTrue(isInLoveRating(RATE_SCORE_IN_LOVE))

        assertTrue(shouldDismissAfterRating(RATE_SCORE_ANGRY))
        assertTrue(shouldDismissAfterRating(RATE_SCORE_SMIRK))
        assertTrue(shouldDismissAfterRating(RATE_SCORE_HAPPY))
        assertFalse(shouldDismissAfterRating(RATE_SCORE_IN_LOVE))
    }
}
