package app.simt.ui.effects

import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CountdownEffectTest {
    @get:Rule
    val compose = createComposeRule()

    private val tick = 1024L
    private val signals = mutableListOf<Pair<Int, Long>>()
    private val view =
        object : View(ApplicationProvider.getApplicationContext()) {
            override fun performHapticFeedback(feedbackConstant: Int): Boolean {
                assertEquals(Looper.getMainLooper(), Looper.myLooper())
                signals.add(feedbackConstant to compose.mainClock.currentTime)
                return true
            }
        }

    @Test
    fun originalOrderAndFullFinalDelayArePreserved() {
        var finished = 0
        var finishTime = -1L
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalView provides view, LocalUiMotionEnabled provides false) {
                    CountdownEffect(Color.Red, tickMs = tick, onFinished = {
                        assertEquals(Looper.getMainLooper(), Looper.myLooper())
                        finishTime = compose.mainClock.currentTime
                        finished++
                    })
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("3").assertExists()
        assertEquals(listOf(HapticFeedbackConstants.CLOCK_TICK), signals.map { it.first })
        val start = signals.single().second
        advanceTo(start + tick - 1)
        assertEquals(1, signals.size)
        assertEquals(0, finished)
        advanceTo(start + tick)
        assertEquals(2, signals.size)
        compose.mainClock.advanceTimeBy(32)
        compose.waitForIdle()
        compose.onNodeWithText("2").assertExists()
        advanceTo(start + tick * 2 - 1)
        assertEquals(2, signals.size)
        advanceTo(start + tick * 2)
        compose.mainClock.advanceTimeBy(32)
        compose.waitForIdle()
        compose.onNodeWithText("1").assertExists()
        assertEquals(
            listOf(HapticFeedbackConstants.CLOCK_TICK, HapticFeedbackConstants.CLOCK_TICK, HapticFeedbackConstants.LONG_PRESS),
            signals.map { it.first },
        )
        assertEquals(listOf(start, start + tick, start + tick * 2), signals.map { it.second })
        advanceTo(start + tick * 3 - 1)
        assertEquals(0, finished)
        advanceTo(start + tick * 3)
        assertEquals(1, finished)
        assertEquals(start + tick * 3, finishTime)
        compose.mainClock.advanceTimeBy(tick * 3)
        assertEquals(1, finished)
        assertEquals(3, signals.size)
    }

    @Test
    fun callbackReplacementAndMotionChangesDoNotRestartCapturedSchedule() {
        val motion = mutableStateOf(true)
        val startValue = mutableStateOf(3)
        val tickMs = mutableStateOf(tick)
        val subtitle = mutableStateOf("Original")
        val starts = AtomicInteger()
        val started = CountDownLatch(1)
        var originalFinish = 0
        var currentFinish = 0
        val finished = mutableStateOf<() -> Unit>({ originalFinish++ })
        val onStart =
            mutableStateOf<suspend () -> Unit>({
                starts.incrementAndGet()
                started.countDown()
            })
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalView provides view, LocalUiMotionEnabled provides motion.value) {
                    CountdownEffect(
                        Color.Red,
                        startValue = startValue.value,
                        tickMs = tickMs.value,
                        onStart = onStart.value,
                        onFinished = finished.value,
                        subtitle = subtitle.value,
                    )
                }
            }
        }
        compose.waitForIdle()
        assertTrue(started.await(5, TimeUnit.SECONDS))
        val start = signals.single().second
        compose.runOnIdle {
            finished.value = { currentFinish++ }
            onStart.value = { starts.addAndGet(100) }
            startValue.value = 9
            tickMs.value = 1L
            subtitle.value = "Replacement"
            motion.value = false
        }
        compose.mainClock.advanceTimeBy(32)
        compose.waitForIdle()
        compose.onNodeWithText("Replacement").assertExists()
        compose.onNodeWithText("3").assertExists()
        advanceTo(start + tick * 2)
        compose.mainClock.advanceTimeBy(32)
        compose.waitForIdle()
        compose.onNodeWithText("1").assertExists()
        assertEquals(3, signals.size)
        advanceTo(start + tick * 3)
        assertEquals(0, originalFinish)
        assertEquals(1, currentFinish)
        assertEquals(1, starts.get())
        compose.mainClock.advanceTimeBy(tick * 3)
        assertEquals(1, currentFinish)
        assertEquals(3, signals.size)
    }

    @Test
    fun removalCancelsCountingAndStructuredBackgroundOnStart() {
        val present = mutableStateOf(true)
        val started = CountDownLatch(1)
        val cancelled = CountDownLatch(1)
        val startLooper = AtomicReference<Looper?>()
        val startThread = AtomicReference<Thread>()
        var finished = 0
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalView provides view, LocalUiMotionEnabled provides false) {
                    if (present.value) {
                        CountdownEffect(
                            Color.Red,
                            tickMs = tick,
                            onStart = {
                                startLooper.set(Looper.myLooper())
                                startThread.set(Thread.currentThread())
                                started.countDown()
                                try {
                                    awaitCancellation()
                                } finally {
                                    cancelled.countDown()
                                }
                            },
                            onFinished = { finished++ },
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        assertTrue(started.await(5, TimeUnit.SECONDS))
        assertFalse(startLooper.get() == Looper.getMainLooper())
        assertTrue(startThread.get().name.contains("DefaultDispatcher"))
        assertEquals(1, signals.size)
        compose.runOnIdle {
            present.value = false
            Snapshot.sendApplyNotifications()
        }
        compose.mainClock.advanceTimeBy(32)
        compose.waitForIdle()
        compose.waitForIdle()
        assertTrue(cancelled.await(5, TimeUnit.SECONDS))
        compose.mainClock.advanceTimeBy(tick * 5)
        assertEquals(0, finished)
        assertEquals(1, signals.size)
        compose.onNodeWithText("3").assertDoesNotExist()
    }

    @Test
    fun hapticPolicyIsCurrentAndIndependentOfMotion() {
        val enabled = mutableStateOf(false)
        var finished = 0
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalView provides view,
                    LocalUiMotionEnabled provides false,
                    LocalUiHapticsEnabled provides enabled.value,
                ) {
                    CountdownEffect(Color.Red, startValue = 2, tickMs = tick, onFinished = { finished++ })
                }
            }
        }
        compose.waitForIdle()
        val start = compose.mainClock.currentTime
        compose.onNodeWithText("2").assertExists()
        assertEquals(0, signals.size)
        compose.runOnIdle { enabled.value = true }
        compose.mainClock.advanceTimeBy(32)
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(tick)
        compose.waitForIdle()
        assertEquals(listOf(HapticFeedbackConstants.LONG_PRESS), signals.map { it.first })
        compose.mainClock.advanceTimeBy(tick)
        compose.waitForIdle()
        assertEquals(1, finished)
        assertTrue(compose.mainClock.currentTime >= start + tick * 2)
    }

    private fun advanceTo(time: Long) {
        compose.mainClock.advanceTimeBy(time - compose.mainClock.currentTime, ignoreFrameDuration = true)
        compose.waitForIdle()
    }
}
