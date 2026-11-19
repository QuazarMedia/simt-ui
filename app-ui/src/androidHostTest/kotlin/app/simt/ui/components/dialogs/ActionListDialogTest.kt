package app.simt.ui.components.dialogs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Regression: a bulk action that layers its own follow-up on top of the list (SET_TAG's prompt)
 * must not tear the dialog down on tap.
 *
 * [ActionListDialog] closes itself after any non-confirm action fires. Actions that need the list
 * to stay mounted while a prompt is layered on top opt out via [ActionListItem.dismissOnSelect]:
 * the tap fires the action but leaves the dialog open, so the caller controls the final dismissal.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ActionListDialogTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `dismissOnSelect defaults to true so existing actions still close the dialog`() {
        assertTrue(
            "the default must preserve the historical fire-then-dismiss behaviour",
            ActionListItem(label = "Favorite", icon = Icons.Default.Star, onClick = {}).dismissOnSelect,
        )
    }

    @Test
    fun `a default action fires its handler and dismisses`() {
        var clicks = 0
        var dismissals = 0
        compose.setContent {
            MaterialTheme {
                ActionListDialog(
                    title = "Group actions",
                    actions = listOf(ActionListItem(label = "Favorite", icon = Icons.Default.Star, onClick = { clicks++ })),
                    onDismiss = { dismissals++ },
                )
            }
        }
        compose.onNodeWithText("Favorite").performTouchInput { click() }

        assertEquals("the action handler must fire", 1, clicks)
        assertEquals("a default action must close the dialog", 1, dismissals)
    }

    @Test
    fun `a dismissOnSelect = false action fires its handler but keeps the dialog open`() {
        var clicks = 0
        var dismissals = 0
        compose.setContent {
            MaterialTheme {
                ActionListDialog(
                    title = "Group actions",
                    actions =
                        listOf(
                            ActionListItem(
                                label = "Set tag",
                                icon = Icons.Default.Star,
                                dismissOnSelect = false,
                                onClick = { clicks++ },
                            ),
                        ),
                    onDismiss = { dismissals++ },
                )
            }
        }
        compose.onNodeWithText("Set tag").performTouchInput { click() }

        assertEquals("the action handler must fire", 1, clicks)
        assertEquals("a dismissOnSelect = false action must leave the dialog open", 0, dismissals)
    }
}
