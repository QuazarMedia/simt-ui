package app.simt.ui.components.buttons

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BasicButtonsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun overlayButtonInvokesOnlyEnabledActions() {
        var enabledCalls = 0
        var disabledCalls = 0
        compose.setContent {
            MaterialTheme {
                Row {
                    OverlayIconButton(
                        icon = Icons.Default.Info,
                        onClick = { enabledCalls++ },
                        contentDescription = "Enabled",
                    )
                    OverlayIconButton(
                        icon = Icons.Default.Info,
                        onClick = { disabledCalls++ },
                        enabled = false,
                        contentDescription = "Disabled",
                    )
                }
            }
        }

        compose.onNodeWithContentDescription("Enabled").performTouchInput { longClick() }
        compose.onNodeWithText("Enabled").assertExists()
        compose.onNodeWithContentDescription("Enabled").performTouchInput { click() }
        compose.onNodeWithContentDescription("Disabled").assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle {
            assertEquals(1, enabledCalls)
            assertEquals(0, disabledCalls)
        }
    }

    @Test
    fun quickActionExposesOneClickableDescriptionForIconAndCaption() {
        var calls = 0
        compose.setContent {
            MaterialTheme {
                QuickActionButton(
                    icon = Icons.Default.Star,
                    label = "Scan",
                    onClick = { calls++ },
                    color = Color.Red,
                )
            }
        }

        compose.onNodeWithContentDescription("Scan").performClick()
        compose.runOnIdle { assertEquals(1, calls) }
    }

    @Test
    fun lockButtonPublishesTheOppositeActionAndTogglesOnce() {
        var locked by mutableStateOf(false)
        val calls = mutableListOf<Boolean>()
        compose.setContent {
            MaterialTheme {
                LockButton(
                    isLocked = locked,
                    onClick = {
                        calls.add(locked)
                        locked = !locked
                    },
                )
            }
        }

        compose.onNodeWithContentDescription("Lock").performTouchInput { click() }
        compose.onNodeWithContentDescription("Unlock").assertExists()
        compose.onNodeWithContentDescription("Lock").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf(false), calls) }
    }

    @Test
    fun favoriteButtonUsesCurrentStateAndShowsTooltipWithoutToggling() {
        var favorite by mutableStateOf(false)
        val calls = mutableListOf<Boolean>()
        compose.setContent {
            MaterialTheme {
                FavoriteToggleButton(
                    isFavorite = favorite,
                    onToggle = {
                        calls.add(it)
                        favorite = it
                    },
                )
            }
        }

        compose.onNodeWithContentDescription("Add to favorites").performTouchInput { click() }
        compose.onNodeWithContentDescription("Remove from favorites").performTouchInput { click() }
        compose.onNodeWithContentDescription("Add to favorites").performTouchInput { longClick() }
        compose.onNodeWithText("Add to favorites").assertExists()
        compose.runOnIdle { assertEquals(listOf(true, false), calls) }
    }

    @Test
    fun visibilityButtonUsesCurrentStateAndShowsTooltipWithoutToggling() {
        var visible by mutableStateOf(false)
        val calls = mutableListOf<Boolean>()
        compose.setContent {
            MaterialTheme {
                VisibilityToggleButton(
                    isVisible = visible,
                    onToggle = {
                        calls.add(it)
                        visible = it
                    },
                )
            }
        }

        compose.onNodeWithContentDescription("Show target").performTouchInput { click() }
        compose.onNodeWithContentDescription("Hide target").performTouchInput { longClick() }
        compose.onNodeWithText("Hide target").assertExists()
        compose.runOnIdle { assertEquals(listOf(true), calls) }
    }
}
