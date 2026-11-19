package app.simt.ui.features.update

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import app.simt.ui.effects.LocalUiMotionEnabled
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
class UpdateAvailableDialogTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun titleAndDescriptionCarryTheCallingAppName() {
        compose.setContent {
            MaterialTheme {
                UpdateAvailableDialog(
                    appName = "Echo",
                    currentVersion = "1.2.3 (42)",
                    onUpdate = {},
                    onDismiss = {},
                )
            }
        }
        compose.onNodeWithText("Echo update available").assertExists()
        compose
            .onNodeWithText("A new version of Echo is available with improvements and fixes. Update now for the best experience.")
            .assertExists()
        compose.onNodeWithText("Current version: 1.2.3 (42)").assertExists()
    }

    @Test
    fun updateAndCancelButtonInvokeTheirOwnCallbacks() {
        var updates = 0
        var dismissals = 0
        compose.setContent {
            MaterialTheme {
                UpdateAvailableDialog(
                    appName = "Echo",
                    currentVersion = "1.0",
                    onUpdate = { updates++ },
                    onDismiss = { dismissals++ },
                )
            }
        }
        compose.onNodeWithTag("update_dialog_update").performTouchInput { click() }
        compose.runOnIdle {
            assertEquals(1, updates)
            assertEquals(0, dismissals)
        }
        compose.onNodeWithTag("update_dialog_cancel").performTouchInput { click() }
        compose.runOnIdle {
            assertEquals(1, updates)
            assertEquals(1, dismissals)
        }
    }

    @Test
    fun disabledMotionRendersTheStaticBannerVariant() {
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalUiMotionEnabled provides false) {
                    UpdateAvailableDialog(
                        appName = "Echo",
                        currentVersion = "1.0",
                        onUpdate = {},
                        onDismiss = {},
                    )
                }
            }
        }
        compose.onNodeWithText("Echo update available").assertExists()
        compose.onNodeWithTag("update_dialog_update").assertExists()
        compose.onNodeWithTag("update_dialog_cancel").assertExists()
    }
}
