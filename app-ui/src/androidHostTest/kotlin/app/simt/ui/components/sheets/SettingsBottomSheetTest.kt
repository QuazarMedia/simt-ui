package app.simt.ui.components.sheets

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.pressBack
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
class SettingsBottomSheetTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun rendersCallerOwnedHeaderAndContent() {
        compose.setContent {
            MaterialTheme {
                SettingsBottomSheet(
                    title = "Track settings",
                    overline = "TRACK+",
                    onDismiss = {},
                ) {
                    Text("Visual tracking")
                }
            }
        }

        compose.onNodeWithText("Track settings").assertIsDisplayed()
        compose.onNodeWithText("TRACK+").assertIsDisplayed()
        compose.onNodeWithText("Visual tracking").assertIsDisplayed()
    }

    @Test
    fun longContentScrollsToTheLastRow() {
        compose.setContent {
            MaterialTheme {
                SettingsBottomSheet(
                    title = "Advanced",
                    onDismiss = {},
                ) {
                    repeat(20) { index ->
                        Text(
                            text = "Option ${index + 1}",
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                        )
                    }
                }
            }
        }

        compose.onNodeWithText("Option 20").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun backDismissalInvokesTheCallerCallback() {
        var dismissals = 0
        compose.setContent {
            MaterialTheme {
                SettingsBottomSheet(
                    title = "Settings",
                    onDismiss = { dismissals++ },
                ) {
                    Text("Music")
                }
            }
        }
        compose.onNodeWithText("Settings").assertExists()

        pressBack()

        compose.waitUntil(timeoutMillis = 5_000) { dismissals == 1 }
        assertEquals(1, dismissals)
    }
}
