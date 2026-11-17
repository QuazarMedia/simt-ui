package app.shared.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import app.shared.ui.effects.LocalUiMotionEnabled
import app.shared.ui.internal.platform.headerWidthFraction
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
class TopOverlayHeaderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun disabledLabeledToolHasNoWrapperClickAndEnabledToolUsesCurrentStateExactlyOnce() {
        val original = mutableListOf<Boolean>()
        val replacement = mutableListOf<Boolean>()
        var disabledCalls = 0
        var closeCalls = 0
        val tools =
            mutableStateOf(
                listOf(
                    TopOverlayToolToggle(false, Icons.Default.Star, onClick = { original.add(it) }, contentDescription = "Toggle"),
                    TopOverlayToolToggle(true, Icons.Default.Info, onClick = { disabledCalls++ }, contentDescription = "Disabled", disabled = true),
                ),
            )
        compose.setContent {
            MaterialTheme {
                TopOverlayHeader("Title", { closeCalls++ }, applyTopInsets = false, tools = tools.value)
            }
        }
        compose.onAllNodes(hasClickAction()).assertCountEquals(3)
        val disabled = compose.onNodeWithContentDescription("Disabled")
        disabled.assertIsNotEnabled().assertIsSelected()
        disabled.performTouchInput { click() }
        disabled.performTouchInput { longClick() }
        compose.onNodeWithText("Disabled").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(0, disabledCalls)
            assertEquals(emptyList<Boolean>(), original)
            assertEquals(0, closeCalls)
        }
        compose.onNodeWithContentDescription("Toggle").assertIsNotSelected().performTouchInput { click() }
        compose.runOnIdle {
            assertEquals(listOf(false), original)
            tools.value =
                tools.value.toMutableList().apply {
                    this[0] =
                        this[0].copy(
                            isOn = true,
                            onClick = { replacement.add(it) },
                            iconTint = Color.Magenta,
                            containerBrush = Brush.linearGradient(listOf(Color.Red, Color.Blue)),
                        )
                }
        }
        compose.onNodeWithContentDescription("Toggle").assertIsSelected().performTouchInput { click() }
        compose.runOnIdle {
            assertEquals(listOf(false), original)
            assertEquals(listOf(true), replacement)
            assertEquals(0, disabledCalls)
        }
        compose.onNodeWithContentDescription("Toggle").performTouchInput { longClick() }
        compose.onNodeWithText("Toggle").assertExists()
        compose.runOnIdle {
            assertEquals(listOf(false), original)
            assertEquals(listOf(true), replacement)
            assertEquals(0, closeCalls)
        }
    }

    @Test
    fun latestSlotSurvivesExitAndReentryCancelsPendingRemoval() {
        val mounted = mutableListOf<String>()
        val disposed = mutableListOf<String>()
        val first: @Composable () -> Unit = {
            DisposableEffect(Unit) {
                mounted.add("first")
                onDispose { disposed.add("first") }
            }
            Text("First", Modifier.testTag("first"))
        }
        val second: @Composable () -> Unit = {
            DisposableEffect(Unit) {
                mounted.add("second")
                onDispose { disposed.add("second") }
            }
            Text("Second", Modifier.testTag("second"))
        }
        val third: @Composable () -> Unit = {
            DisposableEffect(Unit) {
                mounted.add("third")
                onDispose { disposed.add("third") }
            }
            Text("Third", Modifier.testTag("third"))
        }
        val slot = mutableStateOf<(@Composable () -> Unit)?>(first)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme { TopOverlayHeader("Title", {}, applyTopInsets = false, contentCard = slot.value) }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("first").assertExists()
        compose.runOnIdle {
            slot.value = second
            Snapshot.sendApplyNotifications()
        }
        compose.mainClock.advanceTimeBy(32)
        compose.waitForIdle()
        compose.onNodeWithTag("first").assertDoesNotExist()
        compose.onNodeWithTag("second").assertExists()
        compose.runOnIdle {
            slot.value = null
            Snapshot.sendApplyNotifications()
        }
        compose.mainClock.advanceTimeBy(32)
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(160)
        compose.onNodeWithTag("second").assertExists()
        assertEquals(listOf("first", "second"), mounted)
        assertEquals(listOf("first"), disposed)
        compose.runOnIdle {
            slot.value = third
            Snapshot.sendApplyNotifications()
        }
        compose.mainClock.advanceTimeBy(32)
        compose.waitForIdle()
        compose.onNodeWithTag("second").assertDoesNotExist()
        compose.onNodeWithTag("third").assertExists()
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithTag("third").assertExists()
        assertEquals(listOf("first", "second", "third"), mounted)
        assertEquals(listOf("first", "second"), disposed)
        compose.runOnIdle {
            slot.value = null
            Snapshot.sendApplyNotifications()
        }
        compose.mainClock.advanceTimeBy(32)
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(160)
        compose.onNodeWithTag("third").assertExists()
        compose.mainClock.advanceTimeBy(160)
        compose.onNodeWithTag("third").assertDoesNotExist()
        assertEquals(listOf("first", "second", "third"), disposed)
    }

    @Test
    fun firstEntryAndReentryAfterCompletedExitExpandOverTime() {
        val slot = mutableStateOf<(@Composable () -> Unit)?>(null)
        val card: @Composable () -> Unit = { Text("Detail", Modifier.size(96.dp)) }
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                TopOverlayHeader("Title", {}, Modifier.testTag("header"), applyTopInsets = false, contentCard = slot.value)
            }
        }
        compose.waitForIdle()

        fun height() = compose.onNodeWithTag("header").fetchSemanticsNode().boundsInRoot.height
        val emptyHeight = height()
        repeat(2) {
            compose.runOnIdle {
                slot.value = card
                Snapshot.sendApplyNotifications()
            }
            compose.mainClock.advanceTimeBy(96)
            compose.waitForIdle()
            val enteringHeight = height()
            compose.mainClock.advanceTimeBy(400)
            compose.waitForIdle()
            val expandedHeight = height()
            assertTrue("Card did not expand beyond empty header", enteringHeight > emptyHeight)
            assertTrue("Card entered immediately at full height", enteringHeight < expandedHeight - 1f)
            compose.runOnIdle {
                slot.value = null
                Snapshot.sendApplyNotifications()
            }
            compose.mainClock.advanceTimeBy(400)
            compose.waitForIdle()
            assertEquals(emptyHeight, height(), 1f)
        }
    }

    @Test
    fun motionDisabledReleasesSlotImmediatelyAndPreservesAllHeaderSlots() {
        var titleCalls = 0
        var closeCalls = 0
        var disposed = 0
        val slot =
            mutableStateOf<(@Composable () -> Unit)?>(
                {
                    DisposableEffect(Unit) { onDispose { disposed++ } }
                    Text("Card", Modifier.testTag("card"))
                },
            )
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalUiMotionEnabled provides false) {
                    TopOverlayHeader(
                        "Title",
                        { closeCalls++ },
                        modifier = Modifier.size(320.dp, 200.dp).testTag("root"),
                        applyTopInsets = false,
                        subtitle = "Subtitle",
                        icon = { Text("Icon slot") },
                        contentCard = slot.value,
                        onTitleAreaClick = { titleCalls++ },
                    )
                }
            }
        }
        compose.onNodeWithText("Icon slot").assertExists()
        compose.onNodeWithText("Subtitle").assertExists()
        compose.onNodeWithText("Title").performTouchInput { click() }
        compose.onNodeWithContentDescription("Close").performTouchInput { click() }
        compose.runOnIdle {
            slot.value = null
            Snapshot.sendApplyNotifications()
        }
        compose.mainClock.advanceTimeBy(32)
        compose.waitForIdle()
        compose.waitForIdle()
        compose.onNodeWithTag("card").assertDoesNotExist()
        assertEquals(1, disposed)
        assertEquals(1, titleCalls)
        assertEquals(1, closeCalls)
        val root = compose.onNodeWithTag("root").fetchSemanticsNode().boundsInRoot
        assertEquals(with(compose.density) { 320.dp.toPx() }, root.width, 1f)
        assertEquals(with(compose.density) { 200.dp.toPx() }, root.height, 1f)
    }

    @Test
    fun cardCanEnterAnInitiallyEmptySlotAndUpdatesRetainedContentDuringExit() {
        val text = mutableStateOf("Initial detail")
        val slot = mutableStateOf<(@Composable () -> Unit)?>(null)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme { TopOverlayHeader("Title", {}, applyTopInsets = false, contentCard = slot.value) }
        }
        compose.waitForIdle()
        compose.onNodeWithText("Initial detail").assertDoesNotExist()
        compose.runOnIdle {
            slot.value = { Text(text.value) }
            Snapshot.sendApplyNotifications()
        }
        compose.mainClock.advanceTimeBy(320)
        compose.waitForIdle()
        compose.onNodeWithText("Initial detail").assertExists()
        compose.runOnIdle {
            slot.value = null
            Snapshot.sendApplyNotifications()
        }
        compose.mainClock.advanceTimeBy(32)
        compose.waitForIdle()
        compose.runOnIdle {
            text.value = "Current detail"
            Snapshot.sendApplyNotifications()
        }
        compose.mainClock.advanceTimeBy(32)
        compose.waitForIdle()
        compose.onNodeWithText("Initial detail").assertDoesNotExist()
        compose.onNodeWithText("Current detail").assertExists()
        compose.mainClock.advanceTimeBy(320)
        compose.waitForIdle()
        compose.onNodeWithText("Current detail").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "fr")
    fun defaultCloseDescriptionIsTranslated() {
        var closes = 0
        compose.setContent {
            MaterialTheme { TopOverlayHeader("Titre", { closes++ }, applyTopInsets = false) }
        }
        compose.onNodeWithContentDescription("Close").assertDoesNotExist()
        compose.onNodeWithContentDescription("Fermer").performTouchInput { click() }
        assertEquals(1, closes)
    }

    @Test
    fun widthPolicyTracksConfigurationWithoutChangingTheBoundedRoot() {
        val orientation = mutableStateOf(Configuration.ORIENTATION_PORTRAIT)
        var fraction = -1f
        compose.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply { this.orientation = orientation.value }
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                val current = headerWidthFraction()
                SideEffect { fraction = current }
                MaterialTheme {
                    TopOverlayHeader("Title", {}, Modifier.size(320.dp, 100.dp).testTag("root"), applyTopInsets = false)
                }
            }
        }
        compose.waitForIdle()
        assertEquals(1f, fraction)
        val root = compose.onNodeWithTag("root").fetchSemanticsNode().boundsInRoot
        val portraitClose = compose.onNodeWithContentDescription("Close").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { orientation.value = Configuration.ORIENTATION_LANDSCAPE }
        compose.waitForIdle()
        assertEquals(0.5f, fraction)
        val landscapeClose = compose.onNodeWithContentDescription("Close").fetchSemanticsNode().boundsInRoot
        assertEquals(root, compose.onNodeWithTag("root").fetchSemanticsNode().boundsInRoot)
        assertEquals(root.width / 4, portraitClose.right - landscapeClose.right, 1f)
        assertTrue(landscapeClose.right < root.right)
    }
}
