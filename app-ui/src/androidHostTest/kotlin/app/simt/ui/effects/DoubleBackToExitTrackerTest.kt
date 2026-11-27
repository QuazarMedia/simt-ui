package app.simt.ui.effects

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DoubleBackToExitTrackerTest {
    @Test
    fun `first press only arms the tracker`() {
        var now = 10_000L
        val tracker = tracker { now }

        assertFalse(tracker.onBackPressed())
    }

    @Test
    fun `a zero clock timestamp is not treated as a previous press`() {
        val tracker = tracker { 0L }

        assertFalse(tracker.onBackPressed())
    }

    @Test
    fun `second press inside the window confirms the exit`() {
        var now = 10_000L
        val tracker = tracker { now }

        tracker.onBackPressed()
        now = 11_500L

        assertTrue(tracker.onBackPressed())
    }

    @Test
    fun `press after the window is absorbed instead of confirming`() {
        var now = 10_000L
        val tracker = tracker { now }

        tracker.onBackPressed()
        now = 12_001L

        assertFalse(tracker.onBackPressed())
    }

    @Test
    fun `a backwards clock step is absorbed instead of confirming`() {
        var now = 10_000L
        val tracker = tracker { now }

        tracker.onBackPressed()
        now = 9_000L

        assertFalse(tracker.onBackPressed())
    }

    @Test
    fun `a confirmed exit re-arms the tracker`() {
        var now = 10_000L
        val tracker = tracker { now }

        tracker.onBackPressed()
        now = 10_500L
        assertTrue(tracker.onBackPressed())

        // Without the reset this press would fall inside the window of the press that exited.
        now = 11_000L

        assertFalse(tracker.onBackPressed())
    }

    @Test
    fun `the window edge itself is already too late`() {
        var now = 10_000L
        val tracker = tracker { now }

        tracker.onBackPressed()
        now = 12_000L

        assertFalse(tracker.onBackPressed())
    }

    @Test
    fun `the window is configurable`() {
        var now = 10_000L
        val tracker = tracker(windowMs = 10_000L) { now }

        tracker.onBackPressed()
        now = 15_000L

        assertTrue(tracker.onBackPressed())
    }

    private fun tracker(
        windowMs: Long = DEFAULT_WINDOW_MS,
        clock: () -> Long,
    ) = DoubleBackToExitTracker(windowMs = windowMs, clock = clock)

    private companion object {
        const val DEFAULT_WINDOW_MS = 2000L
    }
}
