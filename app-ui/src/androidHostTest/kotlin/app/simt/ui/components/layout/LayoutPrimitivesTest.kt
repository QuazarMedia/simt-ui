package app.simt.ui.components.layout

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
class LayoutPrimitivesTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun infoRowRendersValueAndTrailingSlotWithoutMakingTheRowClickable() {
        var trailingCalls = 0
        compose.setContent {
            MaterialTheme {
                InfoRow(
                    label = "Altitude",
                    value = "1,234 m",
                    icon = Icons.Default.Info,
                    modifier = Modifier.testTag("info"),
                    trailingContent = {
                        Button(onClick = { trailingCalls++ }) { Text("Details") }
                    },
                )
            }
        }

        compose.onNodeWithTag("info").assertHasNoClickAction()
        compose.onNodeWithText("Altitude").assertExists()
        compose.onNodeWithText("1,234 m").assertExists()
        compose.onNodeWithText("Details").performClick()
        compose.runOnIdle { assertEquals(1, trailingCalls) }
    }

    @Test
    fun clickableInfoRowUsesTheLatestCallback() {
        var firstCalls = 0
        var secondCalls = 0
        var useSecond by mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                InfoRow(
                    label = "Open target",
                    icon = Icons.Default.Info,
                    modifier = Modifier.testTag("info"),
                    onClick = {
                        if (useSecond) secondCalls++ else firstCalls++
                    },
                )
            }
        }

        compose.onNodeWithText("Open target").performClick()
        compose.runOnIdle {
            assertEquals(1, firstCalls)
            useSecond = true
        }
        compose.onNodeWithText("Open target").performClick()
        compose.runOnIdle {
            assertEquals(1, firstCalls)
            assertEquals(1, secondCalls)
        }
    }

    @Test
    fun metricBadgeRendersValueAndUppercaseLabelWithoutClickAction() {
        compose.setContent {
            MaterialTheme {
                MetricBadge(
                    icon = Icons.Default.Info,
                    value = "1.2 km",
                    label = "Distance",
                    color = Color.Magenta,
                    modifier = Modifier.testTag("metric"),
                )
            }
        }

        compose.onNodeWithTag("metric").assertHasNoClickAction()
        compose.onNodeWithText("1.2 km").assertExists()
        compose.onNodeWithText("DISTANCE").assertExists()
        compose.onNodeWithText("Distance").assertDoesNotExist()
    }

    @Test
    fun customToolbarScrollsOverflowItemsAndInvokesTheSelectedAction() {
        val calls = mutableListOf<Int>()
        compose.setContent {
            MaterialTheme {
                CustomToolbar(
                    modifier = Modifier.testTag("toolbar"),
                    items =
                        List(12) { index ->
                            {
                                Button(
                                    onClick = { calls.add(index) },
                                    modifier = Modifier.testTag("item_$index"),
                                ) {
                                    Text("Item $index")
                                }
                            }
                        },
                )
            }
        }

        compose.onNodeWithTag("toolbar").assertExists()
        compose.onNodeWithTag("item_11").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf(11), calls) }
    }
}
