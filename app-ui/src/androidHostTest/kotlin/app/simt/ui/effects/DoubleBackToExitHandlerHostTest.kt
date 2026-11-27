package app.simt.ui.effects

import android.app.Activity
import android.os.SystemClock
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.v2.createComposeRule
import app.simt.ui.internal.platform.findActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DoubleBackToExitHandlerHostTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `first back press shows the hint without exiting`() {
        var exitCount = 0
        lateinit var activity: Activity
        compose.setContent {
            val context = LocalContext.current
            SideEffect { activity = requireNotNull(context.findActivity()) }
            DoubleBackToExitHandler(exitHintText = EXIT_HINT, onExit = { exitCount++ })
        }
        compose.waitForIdle()

        compose.runOnIdle { activity.dispatchBack() }
        compose.waitForIdle()

        assertEquals(EXIT_HINT, ShadowToast.getTextOfLatestToast())
        assertFalse(activity.isFinishing)
        assertEquals(0, exitCount)
    }

    @Test
    fun `second back press invokes the custom exit without finishing the activity`() {
        var exitCount = 0
        lateinit var activity: Activity
        compose.setContent {
            val context = LocalContext.current
            SideEffect { activity = requireNotNull(context.findActivity()) }
            DoubleBackToExitHandler(exitHintText = EXIT_HINT, onExit = { exitCount++ })
        }
        compose.waitForIdle()

        compose.runOnIdle { activity.dispatchBack() }
        compose.waitForIdle()
        compose.runOnIdle { activity.dispatchBack() }
        compose.waitForIdle()

        assertEquals(1, exitCount)
        assertFalse(activity.isFinishing)
    }

    @Test
    fun `second back press finishes the activity by default`() {
        lateinit var activity: Activity
        compose.setContent {
            val context = LocalContext.current
            SideEffect { activity = requireNotNull(context.findActivity()) }
            DoubleBackToExitHandler(exitHintText = EXIT_HINT)
        }
        compose.waitForIdle()

        compose.runOnIdle { activity.dispatchBack() }
        compose.waitForIdle()
        assertFalse(activity.isFinishing)

        compose.runOnIdle { activity.dispatchBack() }
        compose.waitForIdle()

        assertTrue(activity.isFinishing)
    }

    @Test
    fun `disabled handler yields back and re-enabling starts a fresh sequence`() {
        val enabled = mutableStateOf(true)
        var fallbackCount = 0
        var exitCount = 0
        lateinit var activity: Activity
        compose.setContent {
            val context = LocalContext.current
            SideEffect { activity = requireNotNull(context.findActivity()) }
            BackHandler { fallbackCount++ }
            DoubleBackToExitHandler(
                exitHintText = EXIT_HINT,
                enabled = enabled.value,
                onExit = { exitCount++ },
            )
        }
        compose.waitForIdle()

        compose.runOnIdle { activity.dispatchBack() }
        compose.waitForIdle()
        assertEquals(EXIT_HINT, ShadowToast.getTextOfLatestToast())

        compose.runOnIdle { enabled.value = false }
        compose.waitForIdle()
        compose.runOnIdle { activity.dispatchBack() }
        compose.waitForIdle()
        assertEquals(1, fallbackCount)
        assertEquals(0, exitCount)
        assertFalse(activity.isFinishing)

        compose.runOnIdle { enabled.value = true }
        compose.waitForIdle()
        compose.runOnIdle { activity.dispatchBack() }
        compose.waitForIdle()

        assertEquals(1, fallbackCount)
        assertEquals(0, exitCount)
        assertEquals(EXIT_HINT, ShadowToast.getTextOfLatestToast())
        assertFalse(activity.isFinishing)
    }

    @Test
    fun `a null hint keeps the absorbed press silent`() {
        var exitCount = 0
        lateinit var activity: Activity
        compose.setContent {
            val context = LocalContext.current
            SideEffect { activity = requireNotNull(context.findActivity()) }
            DoubleBackToExitHandler(onExit = { exitCount++ })
        }
        compose.waitForIdle()

        compose.runOnIdle { activity.dispatchBack() }
        compose.waitForIdle()

        assertNull(ShadowToast.getTextOfLatestToast())
        assertEquals(0, exitCount)
        assertFalse(activity.isFinishing)
    }

    private fun Activity.dispatchBack() {
        val eventTime = SystemClock.uptimeMillis()
        dispatchKeyEvent(
            KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 0),
        )
        dispatchKeyEvent(
            KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK, 0),
        )
    }

    private companion object {
        const val EXIT_HINT = "Press back again to exit"
    }
}
