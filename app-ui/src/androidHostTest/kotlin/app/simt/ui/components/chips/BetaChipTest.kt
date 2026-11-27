package app.simt.ui.components.chips

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BetaChipTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun betaChipRendersCallerSuppliedLabel() {
        compose.setContent {
            MaterialTheme {
                BetaChip(text = "EXPERIMENTAL")
            }
        }

        compose.onNodeWithText("EXPERIMENTAL").assertExists()
    }
}
