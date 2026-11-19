package app.simt.ui.components.dialogs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import app.simt.ui.components.buttons.DialogActionButtons
import org.junit.Assert.assertEquals
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
class ModalFoundationTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun confirmDialogButtonsInvokeTheirOwnCallbacks() {
        var confirmations = 0
        var cancellations = 0
        compose.setContent {
            MaterialTheme {
                ConfirmDialog(
                    onDismissRequest = {},
                    title = "Delete target?",
                    text = "This cannot be undone.",
                    onConfirm = { confirmations++ },
                    onCancel = { cancellations++ },
                )
            }
        }
        compose.onNodeWithText("Delete target?").assertExists()
        compose.onNodeWithText("This cannot be undone.").assertExists()

        compose.onNodeWithText("Cancel").performTouchInput { click() }
        assertEquals(1, cancellations)
        assertEquals(0, confirmations)

        compose.onNodeWithText("Save").performTouchInput { click() }
        assertEquals(1, confirmations)
    }

    @Test
    fun simtDialogRendersContentAndCloseButtonDismisses() {
        var closed = 0
        compose.setContent {
            MaterialTheme {
                SimtDialog(
                    onDismissRequest = {},
                    variant = SimtDialogVariant.Glass,
                    text = { androidx.compose.material3.Text("Something happened.") },
                    onClose = { closed++ },
                )
            }
        }
        compose.onNodeWithText("Something happened.").assertExists()

        // The default dismiss button carries the "Cancel" content description.
        compose.onNodeWithContentDescription("Cancel").performTouchInput { click() }
        assertEquals(1, closed)
    }

    @Test
    fun dialogActionButtonsExposeStableTagsForAutomation() {
        compose.setContent {
            MaterialTheme {
                DialogActionButtons(
                    onCancel = {},
                    onSave = {},
                )
            }
        }
        compose.onNodeWithTag("modal_button_cancel").assertExists()
        compose.onNodeWithTag("modal_button_save").assertExists()

        compose.onNodeWithTag("modal_button_save").performTouchInput { click() }
        compose.onNodeWithTag("modal_button_cancel").performTouchInput { click() }
    }
}
