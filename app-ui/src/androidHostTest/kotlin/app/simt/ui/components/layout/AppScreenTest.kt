package app.simt.ui.components.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun rendersCallerBackgroundHeaderAndContentWithSingleRootModifier() {
        compose.setContent {
            MaterialTheme {
                AppScreen(
                    title = "Targets",
                    subtitle = "Private",
                    onDismissRequest = {},
                    modifier = Modifier.testTag("screen"),
                    headerContentCard = { Text("Header card") },
                    background = { modifier ->
                        Box(modifier = modifier.fillMaxSize().testTag("background")) {
                            Text("Backdrop")
                        }
                    },
                ) {
                    Text("Screen content")
                }
            }
        }

        compose.onAllNodesWithTag("screen").assertCountEquals(1)
        compose.onNodeWithTag("screen").assertIsDisplayed()
        compose.onNodeWithTag("background").assertIsDisplayed()
        compose.onNodeWithText("Backdrop").assertIsDisplayed()
        compose.onNodeWithText("Targets").assertIsDisplayed()
        compose.onNodeWithText("Private").assertIsDisplayed()
        compose.onNodeWithText("Header card").assertIsDisplayed()
        compose.onNodeWithText("Screen content").assertIsDisplayed()
    }

    @Test
    fun headerAndFooterCloseCallbacksStayIndependent() {
        var dismissCalls = 0
        var closeCalls = 0
        compose.setContent {
            MaterialTheme {
                AppScreen(
                    title = "Targets",
                    onDismissRequest = { dismissCalls++ },
                    onClose = { closeCalls++ },
                ) {}
            }
        }

        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNodeWithText("Close").performClick()
        compose.runOnIdle {
            assertEquals(1, dismissCalls)
            assertEquals(1, closeCalls)
        }
    }

    @Test
    fun saveAndCancelActionsForwardCallbacksAndSaveEnabledState() {
        var saveCalls = 0
        var cancelCalls = 0
        var saveEnabled by mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                AppScreen(
                    title = "Edit",
                    onDismissRequest = {},
                    onSave = { saveCalls++ },
                    onCancel = { cancelCalls++ },
                    isSaveEnabled = saveEnabled,
                    saveLabel = "Apply changes",
                    cancelLabel = "Discard",
                ) {}
            }
        }

        compose.onNodeWithTag("modal_button_save").assertIsNotEnabled().performClick()
        compose.onNodeWithText("Discard").performClick()
        compose.runOnIdle {
            assertEquals(0, saveCalls)
            assertEquals(1, cancelCalls)
            saveEnabled = true
        }
        compose.onNodeWithTag("modal_button_save").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, saveCalls) }
    }

    @Test
    fun customActionsReplaceDefaultFooterActions() {
        var customCalls = 0
        var saveCalls = 0
        var closeCalls = 0
        compose.setContent {
            MaterialTheme {
                AppScreen(
                    title = "Targets",
                    onDismissRequest = {},
                    onSave = { saveCalls++ },
                    onClose = { closeCalls++ },
                    saveLabel = "Apply",
                    customActions = {
                        Button(onClick = { customCalls++ }) { Text("Archive") }
                    },
                ) {}
            }
        }

        compose.onNodeWithText("Apply").assertDoesNotExist()
        compose.onNodeWithText("Close").assertDoesNotExist()
        compose.onNodeWithText("Archive").performClick()
        compose.runOnIdle {
            assertEquals(1, customCalls)
            assertEquals(0, saveCalls)
            assertEquals(0, closeCalls)
        }
    }

    @Test
    fun scrollableContentScrollsToOffscreenItems() {
        compose.setContent {
            MaterialTheme {
                AppScreen(
                    title = "Targets",
                    onDismissRequest = {},
                    isScrollable = true,
                ) {
                    Column {
                        repeat(30) { index -> Text("Row $index") }
                    }
                }
            }
        }

        compose.onNodeWithText("Row 29").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun fixedContentDoesNotExposeScrolling() {
        compose.setContent {
            MaterialTheme {
                AppScreen(
                    title = "Targets",
                    onDismissRequest = {},
                    isScrollable = false,
                ) {
                    Text("Fixed content")
                }
            }
        }

        compose.onNodeWithText("Fixed content").assertIsDisplayed()
        compose.onNode(hasScrollAction()).assertDoesNotExist()
    }
}
