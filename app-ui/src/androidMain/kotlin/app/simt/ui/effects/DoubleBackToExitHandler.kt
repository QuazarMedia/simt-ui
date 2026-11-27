package app.simt.ui.effects

import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.simt.ui.internal.platform.findActivity

/** Default window in which a second back press finishes the activity. */
private const val DEFAULT_EXIT_WINDOW_MS = 2000L

/**
 * Absorbs the first back press and finishes the host Activity on a confirming press.
 *
 * Applications choose the hint text and whether confirming back invokes [onExit] instead of
 * finishing the Activity. Disabling the handler discards any pending confirmation.
 *
 * @param exitHintText Localized hint shown on the absorbed press; null keeps it silent.
 * @param enabled Whether the handler intercepts back.
 * @param windowMs Window in which a second press confirms the exit.
 * @param onExit Invoked on confirmation instead of finishing the host Activity.
 */
@Composable
public fun DoubleBackToExitHandler(
    exitHintText: String? = null,
    enabled: Boolean = true,
    windowMs: Long = DEFAULT_EXIT_WINDOW_MS,
    onExit: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val tracker = remember(windowMs, enabled) { DoubleBackToExitTracker(windowMs) }

    BackHandler(enabled = enabled) {
        if (tracker.onBackPressed()) {
            if (onExit != null) onExit() else context.findActivity()?.finish()
        } else if (exitHintText != null) {
            Toast.makeText(context, exitHintText, Toast.LENGTH_SHORT).show()
        }
    }
}

internal class DoubleBackToExitTracker(
    private val windowMs: Long = DEFAULT_EXIT_WINDOW_MS,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) {
    private var lastBackPressTime: Long? = null

    /** @return true when this press confirms the exit and resets the tracker. */
    fun onBackPressed(): Boolean {
        val now = clock()
        val previous = lastBackPressTime
        val confirmed = previous != null && now >= previous && now - previous < windowMs
        lastBackPressTime = if (confirmed) null else now
        return confirmed
    }
}
