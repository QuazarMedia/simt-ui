package app.simt.ui.components.toasts

import android.os.Looper
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Smoke coverage for the toast kit: the surface renders its message, the host stacks every toast in
 * the controller's state, and each entry auto-dismisses after its duration.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class ToastHostTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `toast surface renders its message`() {
        compose.setContent {
            MaterialTheme {
                ToastSurface(
                    message = "Target saved to your list",
                    icon = Icons.Default.Bolt,
                    accentColor = ToastSuccessColor,
                )
            }
        }
        compose.onNodeWithText("Target saved to your list").assertExists()
    }

    @Test
    fun `global toast host renders every toast in the stack`() {
        val controller = FakeToastController()
        compose.setContent {
            MaterialTheme {
                GlobalToastHost(controller = controller)
            }
        }
        controller.showSuccess("Pulse sent")
        controller.showInfo("Peer sent you a Pulse", icon = Icons.Default.Bolt)

        compose.onNodeWithText("Pulse sent").assertExists()
        compose.onNodeWithText("Peer sent you a Pulse").assertExists()
    }

    @Test
    fun `each toast auto-dismisses after its duration`() {
        val controller = FakeToastController()
        compose.setContent {
            MaterialTheme {
                GlobalToastHost(controller = controller)
            }
        }
        controller.showSuccess("Saving")
        compose.onNodeWithText("Saving").assertExists()
        compose.mainClock.autoAdvance = false

        // Drive both the test frame clock (the exit animation) and the main looper (the auto-dismiss
        // delay) in small steps until the stack has drained, bounded so a regression fails the test
        // instead of looping.
        val looper = shadowOf(Looper.getMainLooper())
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (controller.uiState.value.isNotEmpty() && System.nanoTime() < deadline) {
            compose.mainClock.advanceTimeBy(16)
            looper.idleFor(Duration.ofMillis(16))
        }

        assertTrue("the toast must auto-dismiss after its duration", controller.uiState.value.isEmpty())
    }

    private class FakeToastController : ToastController {
        private val nextId = AtomicLong(1L)
        private val _uiState = MutableStateFlow<List<ToastUiState>>(emptyList())
        override val uiState: StateFlow<List<ToastUiState>> = _uiState.asStateFlow()

        override fun showSuccess(message: String) {
            emit(message, ToastStyle.SUCCESS)
        }

        override fun showError(message: String) {
            emit(message, ToastStyle.ERROR)
        }

        override fun showInfo(
            message: String,
            icon: ImageVector,
        ) {
            emit(message, ToastStyle.INFO, icon)
        }

        override fun dismiss(id: Long) {
            _uiState.update { stack -> stack.filterNot { it.id == id } }
        }

        override fun clear() {
            _uiState.value = emptyList()
        }

        private fun emit(
            message: String,
            style: ToastStyle,
            icon: ImageVector? = null,
        ) {
            val toast =
                ToastUiState(
                    id = nextId.getAndIncrement(),
                    message = message,
                    style = style,
                    icon = icon,
                )
            _uiState.update { stack -> stack + toast }
        }
    }
}
