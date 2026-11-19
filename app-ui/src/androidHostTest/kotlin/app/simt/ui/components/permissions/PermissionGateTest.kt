package app.simt.ui.components.permissions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
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
class PermissionGateTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun permissionGateBlocksGetStartedWhileRequirementsUnmet() {
        compose.setContent {
            MaterialTheme {
                PermissionGate(
                    title = "AR measurement",
                    description = "Measure in augmented reality.",
                    icon = Icons.Default.Lock,
                    iconTint = Color.Blue,
                    backgroundStyle = AnimatedDialogBackgroundStyle.Geometric,
                    requirements =
                        listOf(
                            FeatureRequirement(
                                label = "ARCore",
                                status = RequirementStatus.UNAVAILABLE,
                                description = "ARCore is not installed.",
                            ),
                        ),
                    onGetStarted = {},
                    onCancel = {},
                    requirementsNotMetText = "Device not supported.",
                )
            }
        }
        compose.onNodeWithText("ARCore").assertExists()
        compose.onNodeWithText("MISSING").assertExists()
        compose.onNodeWithText("Device not supported.").assertExists()
        compose.onNodeWithText("GET STARTED").assertDoesNotExist()
    }

    @Test
    fun permissionGateInvokesGetStartedWhenRequirementsMet() {
        var started = 0
        compose.setContent {
            MaterialTheme {
                PermissionGate(
                    title = "Camera access",
                    description = "Align your target with the stars.",
                    icon = Icons.Default.Lock,
                    iconTint = Color.Blue,
                    backgroundStyle = AnimatedDialogBackgroundStyle.RadarSweep,
                    permissionsExplanation = "Used only while this screen is open.",
                    onGetStarted = { started++ },
                    onCancel = {},
                )
            }
        }
        compose.onNodeWithText("Used only while this screen is open.").assertExists()

        compose.onNodeWithText("GET STARTED").performTouchInput { click() }
        assertEquals(1, started)
    }

    @Test
    fun permissionGateRendersGrantCopyLikeTheRequestDialog() {
        // PermissionRequestDialog resolves to this exact gate configuration; assert the
        // grant-copy path (button copy, container/content colors) without a second dialog.
        var grants = 0
        compose.setContent {
            MaterialTheme {
                PermissionGate(
                    title = "Bluetooth devices",
                    description = "",
                    icon = Icons.Default.Lock,
                    iconTint = Color.Blue,
                    backgroundStyle = AnimatedDialogBackgroundStyle.NetworkGrid,
                    permissionsExplanation = "Sync with a paired watch.",
                    onGetStarted = { grants++ },
                    onCancel = {},
                    getStartedButtonText = "Grant Permission",
                    cancelButtonText = "Cancel",
                    getStartedContainerColor = Color.Blue,
                    getStartedContentColor = Color.White,
                )
            }
        }
        compose.onNodeWithText("Sync with a paired watch.").assertExists()
        compose.onNodeWithText("GRANT PERMISSION").performTouchInput { click() }
        assertEquals(1, grants)
    }

    @Test
    fun featureRequirementCardSurfacesFixDialog() {
        var fixes = 0
        compose.setContent {
            MaterialTheme {
                FeatureRequirementCard(
                    requirement =
                        FeatureRequirement(
                            label = "ARCore",
                            status = RequirementStatus.PARTIAL,
                            description = "Install ARCore from the Play Store.",
                            onFix = { fixes++ },
                        ),
                )
            }
        }
        compose.onNodeWithText("ARCore").assertExists()
        compose.onNodeWithText("LIMITED").assertExists()

        // The card is clickable when it has a description; tapping opens the info dialog.
        compose.onNodeWithText("Install ARCore from the Play Store.").assertDoesNotExist()
        compose.onNodeWithText("ARCore").performTouchInput { click() }
        compose.onNodeWithText("Install ARCore from the Play Store.").assertExists()

        compose.onNodeWithText("Fix it").performTouchInput { click() }
        assertEquals(1, fixes)
    }
}
