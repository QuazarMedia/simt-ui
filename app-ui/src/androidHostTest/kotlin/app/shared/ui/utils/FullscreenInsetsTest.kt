package app.shared.ui.utils

import android.graphics.Rect
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.DisplayCutoutCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FullscreenInsetsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun hiddenBarsAndCutoutAreUnionedOnCorrectSidesAndConsumedOnlyOnce() {
        lateinit var view: View
        compose.setContent {
            val current = LocalView.current
            SideEffect { view = current }
            Box(Modifier.size(240.dp).testTag("root")) {
                Box(Modifier.fillMaxSize().fullScreenTopInsets().fullScreenBottomInsets().testTag("padded")) {
                    Box(Modifier.fillMaxSize().fullScreenTopInsets().fullScreenBottomInsets().testTag("nested")) {
                        Box(Modifier.fillMaxSize().testTag("leaf"))
                    }
                }
            }
        }
        compose.waitForIdle()
        val insets =
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.NONE)
                .setInsetsIgnoringVisibility(WindowInsetsCompat.Type.statusBars(), Insets.of(0, 24, 0, 0))
                .setVisible(WindowInsetsCompat.Type.statusBars(), false)
                .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.NONE)
                .setInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, 32))
                .setVisible(WindowInsetsCompat.Type.navigationBars(), false)
                .setDisplayCutout(DisplayCutoutCompat(Insets.of(0, 40, 0, 50), null, Rect(50, 0, 100, 40), null, Rect(50, 950, 100, 1000), Insets.NONE))
                .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, 200))
                .setVisible(WindowInsetsCompat.Type.ime(), true)
                .build()
        compose.runOnIdle { ViewCompat.dispatchApplyWindowInsets(view, insets) }
        compose.waitForIdle()
        val root = compose.onNodeWithTag("root").fetchSemanticsNode().boundsInRoot
        val nested = compose.onNodeWithTag("nested").fetchSemanticsNode().boundsInRoot
        val leaf = compose.onNodeWithTag("leaf").fetchSemanticsNode().boundsInRoot
        assertEquals(40f, nested.top - root.top, 0.5f)
        assertEquals(50f, root.bottom - nested.bottom, 0.5f)
        assertEquals(nested, leaf)
        assertTrue(leaf.height > 0)
    }

    @Test
    fun ignoringVisibilityBarsStillPadWithoutCutoutAndImeDoesNotAffectBottom() {
        lateinit var view: View
        compose.setContent {
            val current = LocalView.current
            SideEffect { view = current }
            Box(Modifier.size(240.dp).testTag("root")) {
                Box(Modifier.fillMaxSize().fullScreenTopInsets().fullScreenBottomInsets()) {
                    Box(Modifier.fillMaxSize().testTag("leaf"))
                }
            }
        }
        compose.waitForIdle()

        fun injected(ime: Int): WindowInsetsCompat =
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.NONE)
                .setInsetsIgnoringVisibility(WindowInsetsCompat.Type.statusBars(), Insets.of(7, 28, 9, 0))
                .setInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars(), Insets.of(7, 0, 9, 36))
                .setVisible(WindowInsetsCompat.Type.systemBars(), false)
                .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, ime))
                .setVisible(WindowInsetsCompat.Type.ime(), ime > 0)
                .build()
        compose.runOnIdle { ViewCompat.dispatchApplyWindowInsets(view, injected(200)) }
        compose.waitForIdle()
        val root = compose.onNodeWithTag("root").fetchSemanticsNode().boundsInRoot
        val leaf = compose.onNodeWithTag("leaf").fetchSemanticsNode().boundsInRoot
        assertEquals(28f, leaf.top - root.top, 0.5f)
        assertEquals(36f, root.bottom - leaf.bottom, 0.5f)
        assertEquals(root.width, leaf.width, 0.5f)
        compose.runOnIdle { ViewCompat.dispatchApplyWindowInsets(view, injected(0)) }
        compose.waitForIdle()
        assertEquals(leaf, compose.onNodeWithTag("leaf").fetchSemanticsNode().boundsInRoot)
    }
}
