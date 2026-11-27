package app.simt.ui.effects

import android.app.Activity
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.simt.ui.internal.platform.findActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FullScreenExperienceEffectHostTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `effect restores existing keep screen on policy and visible bars`() {
        val enabled = mutableStateOf(false)
        lateinit var activity: Activity
        compose.setContent {
            val context = LocalContext.current
            SideEffect { activity = requireNotNull(context.findActivity()) }
            if (enabled.value) FullScreenExperienceEffect()
        }
        compose.waitForIdle()

        val window = activity.window
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        compose.runOnIdle {
            window.decorView.keepScreenOn = true
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            controller.show(WindowInsetsCompat.Type.systemBars())
            enabled.value = true
        }
        compose.waitForIdle()

        assertTrue(window.decorView.keepScreenOn)
        assertTrue(window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0)
        assertEquals(
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE,
            controller.systemBarsBehavior,
        )
        assertFalse(window.isVisible(WindowInsetsCompat.Type.statusBars()))
        assertFalse(window.isVisible(WindowInsetsCompat.Type.navigationBars()))

        compose.runOnIdle { enabled.value = false }
        compose.waitForIdle()

        assertTrue(window.decorView.keepScreenOn)
        assertTrue(window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0)
        assertEquals(WindowInsetsControllerCompat.BEHAVIOR_DEFAULT, controller.systemBarsBehavior)
        assertTrue(window.isVisible(WindowInsetsCompat.Type.statusBars()))
        assertTrue(window.isVisible(WindowInsetsCompat.Type.navigationBars()))
    }

    @Test
    @Suppress("DEPRECATION")
    fun `effect restores each bar independently without clearing absent flags`() {
        val enabled = mutableStateOf(false)
        lateinit var activity: Activity
        compose.setContent {
            val context = LocalContext.current
            SideEffect { activity = requireNotNull(context.findActivity()) }
            if (enabled.value) {
                FullScreenExperienceEffect(desiredSystemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_TOUCH)
            }
        }
        compose.waitForIdle()

        val window = activity.window
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        compose.runOnIdle {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            controller.show(WindowInsetsCompat.Type.systemBars())
            controller.hide(WindowInsetsCompat.Type.statusBars())
            enabled.value = true
        }
        compose.waitForIdle()

        assertTrue(window.decorView.keepScreenOn)
        assertTrue(window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0)
        assertFalse(window.isVisible(WindowInsetsCompat.Type.statusBars()))
        assertFalse(window.isVisible(WindowInsetsCompat.Type.navigationBars()))

        compose.runOnIdle { enabled.value = false }
        compose.waitForIdle()

        assertFalse(window.decorView.keepScreenOn)
        assertTrue(window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON == 0)
        assertEquals(WindowInsetsControllerCompat.BEHAVIOR_DEFAULT, controller.systemBarsBehavior)
        assertFalse(window.isVisible(WindowInsetsCompat.Type.statusBars()))
        assertTrue(window.isVisible(WindowInsetsCompat.Type.navigationBars()))
    }

    @Test
    @Suppress("DEPRECATION")
    fun `dispose re-hides bars that were hidden before a transient reveal`() {
        val enabled = mutableStateOf(false)
        lateinit var activity: Activity
        compose.setContent {
            val context = LocalContext.current
            SideEffect { activity = requireNotNull(context.findActivity()) }
            if (enabled.value) FullScreenExperienceEffect(keepScreenOn = false)
        }
        compose.waitForIdle()

        val window = activity.window
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        compose.runOnIdle {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            controller.show(WindowInsetsCompat.Type.systemBars())
            controller.hide(WindowInsetsCompat.Type.statusBars())
            enabled.value = true
        }
        compose.waitForIdle()
        assertFalse(window.isVisible(WindowInsetsCompat.Type.statusBars()))
        assertFalse(window.isVisible(WindowInsetsCompat.Type.navigationBars()))

        compose.runOnIdle { controller.show(WindowInsetsCompat.Type.systemBars()) }
        compose.waitForIdle()
        assertTrue(window.isVisible(WindowInsetsCompat.Type.statusBars()))
        assertTrue(window.isVisible(WindowInsetsCompat.Type.navigationBars()))

        compose.runOnIdle { enabled.value = false }
        compose.waitForIdle()
        assertFalse(window.isVisible(WindowInsetsCompat.Type.statusBars()))
        assertTrue(window.isVisible(WindowInsetsCompat.Type.navigationBars()))
    }

    @Test
    @Suppress("DEPRECATION")
    fun `overlapping owners keep policy active and fall back to the remaining behavior`() {
        val firstEnabled = mutableStateOf(false)
        val secondEnabled = mutableStateOf(false)
        lateinit var activity: Activity
        compose.setContent {
            val context = LocalContext.current
            SideEffect { activity = requireNotNull(context.findActivity()) }
            if (firstEnabled.value) {
                FullScreenExperienceEffect(desiredSystemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT)
            }
            if (secondEnabled.value) FullScreenExperienceEffect()
        }
        compose.waitForIdle()

        val window = activity.window
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        compose.runOnIdle {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_TOUCH
            controller.show(WindowInsetsCompat.Type.systemBars())
            firstEnabled.value = true
        }
        compose.waitForIdle()
        compose.runOnIdle { secondEnabled.value = true }
        compose.waitForIdle()

        assertTrue(window.decorView.keepScreenOn)
        assertEquals(
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE,
            controller.systemBarsBehavior,
        )
        assertFalse(window.isVisible(WindowInsetsCompat.Type.statusBars()))
        assertFalse(window.isVisible(WindowInsetsCompat.Type.navigationBars()))

        compose.runOnIdle { secondEnabled.value = false }
        compose.waitForIdle()
        assertTrue(window.decorView.keepScreenOn)
        assertTrue(window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0)
        assertEquals(WindowInsetsControllerCompat.BEHAVIOR_DEFAULT, controller.systemBarsBehavior)
        assertFalse(window.isVisible(WindowInsetsCompat.Type.statusBars()))
        assertFalse(window.isVisible(WindowInsetsCompat.Type.navigationBars()))

        compose.runOnIdle { firstEnabled.value = false }
        compose.waitForIdle()
        assertFalse(window.decorView.keepScreenOn)
        assertTrue(window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON == 0)
        assertEquals(WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_TOUCH, controller.systemBarsBehavior)
        assertTrue(window.isVisible(WindowInsetsCompat.Type.statusBars()))
        assertTrue(window.isVisible(WindowInsetsCompat.Type.navigationBars()))
    }

    @Test
    fun `a remaining owner with disabled requests does not retain another owner's policy`() {
        val policyEnabled = mutableStateOf(false)
        val disabledOwnerEnabled = mutableStateOf(false)
        lateinit var activity: Activity
        compose.setContent {
            val context = LocalContext.current
            SideEffect { activity = requireNotNull(context.findActivity()) }
            if (policyEnabled.value) FullScreenExperienceEffect()
            if (disabledOwnerEnabled.value) {
                FullScreenExperienceEffect(keepScreenOn = false, hideSystemBars = false)
            }
        }
        compose.waitForIdle()

        val window = activity.window
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        compose.runOnIdle {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller.show(WindowInsetsCompat.Type.systemBars())
            policyEnabled.value = true
        }
        compose.waitForIdle()
        compose.runOnIdle { disabledOwnerEnabled.value = true }
        compose.waitForIdle()
        assertTrue(window.decorView.keepScreenOn)
        assertFalse(window.isVisible(WindowInsetsCompat.Type.statusBars()))

        compose.runOnIdle { policyEnabled.value = false }
        compose.waitForIdle()
        assertFalse(window.decorView.keepScreenOn)
        assertTrue(window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON == 0)
        assertTrue(window.isVisible(WindowInsetsCompat.Type.statusBars()))
        assertTrue(window.isVisible(WindowInsetsCompat.Type.navigationBars()))

        compose.runOnIdle { disabledOwnerEnabled.value = false }
        compose.waitForIdle()
        assertFalse(window.decorView.keepScreenOn)
        assertTrue(window.isVisible(WindowInsetsCompat.Type.statusBars()))
        assertTrue(window.isVisible(WindowInsetsCompat.Type.navigationBars()))
    }

    @Test
    fun `always show on dispose reveals both bars without changing disabled keep screen on`() {
        val enabled = mutableStateOf(false)
        val alwaysShow = mutableStateOf(false)
        lateinit var activity: Activity
        compose.setContent {
            val context = LocalContext.current
            SideEffect { activity = requireNotNull(context.findActivity()) }
            if (enabled.value) {
                FullScreenExperienceEffect(
                    keepScreenOn = false,
                    hideSystemBars = true,
                    alwaysShowSystemBarsOnDispose = alwaysShow.value,
                )
            }
        }
        compose.waitForIdle()

        val window = activity.window
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        compose.runOnIdle {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            alwaysShow.value = true
            enabled.value = true
        }
        compose.waitForIdle()
        assertFalse(window.decorView.keepScreenOn)
        assertTrue(window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON == 0)

        compose.runOnIdle { enabled.value = false }
        compose.waitForIdle()
        assertTrue(window.isVisible(WindowInsetsCompat.Type.statusBars()))
        assertTrue(window.isVisible(WindowInsetsCompat.Type.navigationBars()))
        assertFalse(window.decorView.keepScreenOn)
        assertTrue(window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON == 0)
    }

    @Test
    fun `disabled fullscreen requests leave host policies untouched`() {
        val enabled = mutableStateOf(false)
        lateinit var activity: Activity
        compose.setContent {
            val context = LocalContext.current
            SideEffect { activity = requireNotNull(context.findActivity()) }
            if (enabled.value) {
                FullScreenExperienceEffect(keepScreenOn = false, hideSystemBars = false)
            }
        }
        compose.waitForIdle()

        val window = activity.window
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        compose.runOnIdle {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.show(WindowInsetsCompat.Type.systemBars())
            controller.hide(WindowInsetsCompat.Type.statusBars())
            enabled.value = true
        }
        compose.waitForIdle()

        assertFalse(window.decorView.keepScreenOn)
        assertTrue(window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON == 0)
        assertEquals(
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE,
            controller.systemBarsBehavior,
        )
        assertFalse(window.isVisible(WindowInsetsCompat.Type.statusBars()))
        assertTrue(window.isVisible(WindowInsetsCompat.Type.navigationBars()))
    }

    @Test
    fun `dialog content controls only its own window and restores it`() {
        val enabled = mutableStateOf(false)
        lateinit var dialogWindow: Window
        lateinit var dialogView: View
        lateinit var activity: Activity
        compose.setContent {
            val context = LocalContext.current
            SideEffect { activity = requireNotNull(context.findActivity()) }
            Dialog(onDismissRequest = {}) {
                val view = LocalView.current
                SideEffect {
                    dialogWindow = (view.parent as DialogWindowProvider).window
                    dialogView = dialogWindow.decorView
                }
                if (enabled.value) FullScreenExperienceEffect()
            }
        }
        compose.waitForIdle()

        val controller = WindowInsetsControllerCompat(dialogWindow, dialogView)
        compose.runOnIdle {
            controller.show(WindowInsetsCompat.Type.systemBars())
            enabled.value = true
        }
        compose.waitForIdle()
        assertTrue(dialogView.keepScreenOn)
        assertTrue(dialogWindow.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0)
        assertFalse(dialogWindow.isVisible(WindowInsetsCompat.Type.statusBars()))
        assertFalse(dialogWindow.isVisible(WindowInsetsCompat.Type.navigationBars()))
        assertFalse(activity.window.decorView.keepScreenOn)
        assertTrue(activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON == 0)

        compose.runOnIdle { enabled.value = false }
        compose.waitForIdle()
        assertFalse(dialogView.keepScreenOn)
        assertTrue(dialogWindow.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON == 0)
        assertTrue(dialogWindow.isVisible(WindowInsetsCompat.Type.statusBars()))
        assertTrue(dialogWindow.isVisible(WindowInsetsCompat.Type.navigationBars()))
    }

    private fun Window.isVisible(type: Int): Boolean = ViewCompat.getRootWindowInsets(decorView)?.isVisible(type) == true
}
