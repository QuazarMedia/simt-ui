package app.simt.ui.effects

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.simt.ui.components.loader.BlockingLoaderController
import app.simt.ui.components.loader.BlockingLoaderUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Smoke coverage for the global blocking loader host: hidden renders nothing, visible renders the
 * message and the interrupt affordance, and the background-processing warning banner shows its
 * custom message and can be dismissed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class GlobalBlockingLoaderHostTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `hidden loader renders nothing`() {
        val controller = FakeBlockingLoaderController()
        compose.setContent {
            MaterialTheme {
                GlobalBlockingLoaderHost(controller = controller)
            }
        }

        // Hidden by default: nothing is composed.
        compose.onNodeWithText("Saving").assertDoesNotExist()

        controller.show(message = "Saving")
        compose.onNodeWithText("Saving").assertExists()

        controller.clear()
        compose.onNodeWithText("Saving").assertDoesNotExist()
    }

    @Test
    fun `visible loader renders the message and the interrupt button`() {
        val controller = FakeBlockingLoaderController()
        compose.setContent {
            MaterialTheme {
                GlobalBlockingLoaderHost(controller = controller)
            }
        }

        controller.show(message = "Uploading", onInterrupt = { true })
        compose.onNodeWithText("Uploading").assertExists()

        val cancelButton = compose.onNodeWithContentDescription("Cancel")
        cancelButton.assertExists()
        cancelButton.performClick()
        assertTrue("the interrupt request must be forwarded to the controller", controller.interrupted)
    }

    @Test
    fun `background processing warning banner shows its message and dismisses`() {
        val controller = FakeBlockingLoaderController()
        compose.setContent {
            MaterialTheme {
                GlobalBlockingLoaderHost(controller = controller)
            }
        }

        controller.show(
            showBackgroundProcessingWarning = true,
            backgroundProcessingWarningMessage = "Stay in the app",
        )
        val banner = compose.onNodeWithText("Stay in the app")
        banner.assertExists()

        // The banner's close button carries the shared "Close" content description.
        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNodeWithText("Stay in the app").assertDoesNotExist()
    }

    private class FakeBlockingLoaderController : BlockingLoaderController {
        private val _uiState = MutableStateFlow(BlockingLoaderUiState(visible = false))
        override val uiState: StateFlow<BlockingLoaderUiState> = _uiState.asStateFlow()

        @Volatile
        var interrupted = false

        override fun show(
            message: String?,
            progress: Float?,
            onInterrupt: (suspend () -> Boolean)?,
            showBackgroundProcessingWarning: Boolean,
            backgroundProcessingWarningMessage: String?,
        ): String {
            _uiState.value =
                BlockingLoaderUiState(
                    visible = true,
                    message = message,
                    progress = progress,
                    onInterrupt = onInterrupt,
                    showBackgroundProcessingWarning = showBackgroundProcessingWarning,
                    backgroundProcessingWarningMessage = backgroundProcessingWarningMessage,
                )
            return "token"
        }

        override fun update(
            token: String,
            message: String?,
            progress: Float?,
            showBackgroundProcessingWarning: Boolean?,
            backgroundProcessingWarningMessage: String?,
        ) {
            _uiState.update {
                it.copy(
                    message = message ?: it.message,
                    progress = progress ?: it.progress,
                    showBackgroundProcessingWarning =
                        showBackgroundProcessingWarning ?: it.showBackgroundProcessingWarning,
                    backgroundProcessingWarningMessage =
                        backgroundProcessingWarningMessage ?: it.backgroundProcessingWarningMessage,
                )
            }
        }

        override fun hide(token: String) {
            _uiState.update { it.copy(visible = false) }
        }

        override fun clear() {
            _uiState.value = BlockingLoaderUiState(visible = false)
        }

        override suspend fun requestInterrupt(): Boolean {
            interrupted = true
            return true
        }
    }
}
