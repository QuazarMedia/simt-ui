package app.simt.ui.effects

import android.app.Activity
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.v2.createComposeRule
import app.simt.ui.internal.platform.findActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LockCurrentOrientationEffectHostTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `effect locks the host activity and restores its previous policy on disposal`() {
        val locked = mutableStateOf(false)
        lateinit var activity: Activity
        compose.setContent {
            val context = LocalContext.current
            SideEffect { activity = requireNotNull(context.findActivity()) }
            if (locked.value) LockCurrentOrientationEffect()
        }
        compose.waitForIdle()

        compose.runOnIdle {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            locked.value = true
        }
        compose.waitForIdle()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LOCKED, activity.requestedOrientation)

        compose.runOnIdle { locked.value = false }
        compose.waitForIdle()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT, activity.requestedOrientation)
    }

    @Test
    fun `overlapping effect owners restore only after the final disposal`() {
        val firstLocked = mutableStateOf(false)
        val secondLocked = mutableStateOf(false)
        lateinit var activity: Activity
        compose.setContent {
            val context = LocalContext.current
            SideEffect { activity = requireNotNull(context.findActivity()) }
            if (firstLocked.value) LockCurrentOrientationEffect()
            if (secondLocked.value) LockCurrentOrientationEffect()
        }
        compose.waitForIdle()

        compose.runOnIdle {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            firstLocked.value = true
            secondLocked.value = true
        }
        compose.waitForIdle()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LOCKED, activity.requestedOrientation)

        compose.runOnIdle { firstLocked.value = false }
        compose.waitForIdle()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LOCKED, activity.requestedOrientation)

        compose.runOnIdle { secondLocked.value = false }
        compose.waitForIdle()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, activity.requestedOrientation)
    }

    @Test
    fun `activity lookup walks nested context wrappers`() {
        lateinit var activity: Activity
        compose.setContent {
            val context = LocalContext.current
            SideEffect { activity = requireNotNull(context.findActivity()) }
        }
        compose.waitForIdle()

        assertSame(activity, ContextWrapper(ContextWrapper(activity)).findActivity())
    }
}
