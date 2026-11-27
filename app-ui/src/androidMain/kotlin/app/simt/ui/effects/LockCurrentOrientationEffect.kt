package app.simt.ui.effects

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import app.simt.ui.internal.platform.findActivity
import java.util.IdentityHashMap

/**
 * Requests a lock of the host activity's current physical orientation while this composable is active.
 *
 * Overlapping callers share reference-counted ownership. The exact previous orientation policy is
 * restored when the final caller leaves composition. Android can ignore requested-orientation
 * changes on large displays or in restricted window modes, so physical-device behavior remains
 * host-configuration validation.
 */
@Composable
public fun LockCurrentOrientationEffect() {
    val activity = LocalContext.current.findActivity()

    DisposableEffect(activity) {
        if (activity != null) {
            activityOrientationLockCoordinator.acquire(activity)
        }
        onDispose {
            if (activity != null) {
                activityOrientationLockCoordinator.release(activity)
            }
        }
    }
}

internal class OrientationLockCoordinator<T : Any>(
    private val lockedOrientation: Int,
    private val readOrientation: (T) -> Int,
    private val writeOrientation: (T, Int) -> Unit,
) {
    private data class LockState(
        val previousOrientation: Int,
        var ownerCount: Int,
    )

    private val states = IdentityHashMap<T, LockState>()

    @Synchronized
    fun acquire(target: T) {
        val state = states[target]
        if (state != null) {
            state.ownerCount += 1
            return
        }

        val previousOrientation = readOrientation(target)
        writeOrientation(target, lockedOrientation)
        states[target] = LockState(previousOrientation = previousOrientation, ownerCount = 1)
    }

    @Synchronized
    fun release(target: T) {
        val state = states[target] ?: return
        state.ownerCount -= 1
        if (state.ownerCount > 0) return

        states.remove(target)
        writeOrientation(target, state.previousOrientation)
    }
}

private val activityOrientationLockCoordinator =
    OrientationLockCoordinator<Activity>(
        lockedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED,
        readOrientation = Activity::getRequestedOrientation,
        writeOrientation = Activity::setRequestedOrientation,
    )
