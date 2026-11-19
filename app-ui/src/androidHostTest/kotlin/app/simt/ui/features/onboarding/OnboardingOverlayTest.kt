package app.simt.ui.features.onboarding

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Coverage for the onboarding spotlight: the text-placement rule (ported from simt-app's
 * OnboardingOverlayTest) plus smoke coverage of the [enabled] gate and the dismiss callback.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class OnboardingOverlayTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `text stays at bottom when focus is in top half`() {
        val focusedBounds = Rect(left = 0f, top = 100f, right = 200f, bottom = 300f)

        assertFalse(shouldPlaceOnboardingTextAtTop(focusedBounds, overlayHeightPx = 1_000f))
    }

    @Test
    fun `text moves to top when focus is in bottom half`() {
        val focusedBounds = Rect(left = 0f, top = 600f, right = 200f, bottom = 800f)

        assertTrue(shouldPlaceOnboardingTextAtTop(focusedBounds, overlayHeightPx = 1_000f))
    }

    @Test
    fun `screen midpoint belongs to bottom half`() {
        val focusedBounds = Rect(left = 0f, top = 450f, right = 200f, bottom = 550f)

        assertTrue(shouldPlaceOnboardingTextAtTop(focusedBounds, overlayHeightPx = 1_000f))
    }

    @Test
    fun `disabled overlay renders nothing`() {
        compose.setContent {
            MaterialTheme {
                OnboardingOverlay(
                    enabled = false,
                    highlightedBounds = Rect(100f, 100f, 300f, 300f),
                    onDismiss = {},
                    title = "Getting started",
                    description = "Tap to continue",
                )
            }
        }

        compose.onNodeWithText("Tap to continue").assertDoesNotExist()
        compose.onNodeWithText("GETTING STARTED").assertDoesNotExist()
    }

    @Test
    fun `enabled overlay renders the title and description and dismisses on click`() {
        var dismissed = false
        compose.setContent {
            MaterialTheme {
                OnboardingOverlay(
                    highlightedBounds = Rect(100f, 100f, 300f, 300f),
                    onDismiss = { dismissed = true },
                    title = "Getting started",
                    description = "Tap to continue",
                )
            }
        }

        compose.onNodeWithText("GETTING STARTED").assertExists()
        val description = compose.onNodeWithText("Tap to continue")
        description.assertExists()

        description.performClick()
        assertTrue("clicking the scrim must invoke onDismiss", dismissed)
    }
}
