package app.simt.ui.components.layout

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EditableItemCardTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun rendersCallerContentAndWarningWithExternalModifier() {
        compose.setContent {
            MaterialTheme {
                EditableItemCard(
                    icon = Icons.Default.Info,
                    title = "Work",
                    subtitle = "4 targets",
                    description = "Private scope",
                    warningMessage = "Target limit reached",
                    modifier = Modifier.testTag("editable-item"),
                    isEditing = false,
                    isEditEnabled = true,
                    isDeleteEnabled = true,
                    editLabel = "Edit tag",
                    deleteLabel = "Delete tag",
                    onEditSave = {},
                    onEditCancel = {},
                )
            }
        }

        compose.onNodeWithTag("editable-item").assertIsDisplayed()
        compose.onNodeWithText("Work").assertIsDisplayed()
        compose.onNodeWithText("4 targets").assertIsDisplayed()
        compose.onNodeWithText("Private scope").assertIsDisplayed()
        compose.onNodeWithText("Target limit reached").assertIsDisplayed()
    }

    @Test
    fun titleViewEditAndDeleteActionsStayIndependentAndUseCallerLabels() {
        val calls = mutableListOf<String>()
        compose.setContent {
            MaterialTheme {
                EditableItemCard(
                    icon = Icons.Default.Info,
                    title = "Travel",
                    isEditing = false,
                    isEditEnabled = true,
                    isDeleteEnabled = true,
                    editLabel = "Edit tag",
                    deleteLabel = "Delete tag",
                    onView =
                        EditableItemViewAction(
                            onClick = { calls.add("view") },
                            contentDescription = "View targets",
                            icon = Icons.Default.LocationOn,
                        ),
                    onTitleClick = { calls.add("title") },
                    titleClickLabel = "Show on map",
                    onStartEdit = { calls.add("edit") },
                    onDelete = { calls.add("delete") },
                    onEditSave = {},
                    onEditCancel = {},
                )
            }
        }

        compose.onNodeWithText("Travel").performClick()
        compose.onNodeWithContentDescription("View targets")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        compose.onNodeWithContentDescription("Edit tag")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        compose.onNodeWithContentDescription("Delete tag")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        compose.runOnIdle { assertEquals(listOf("title", "view", "edit", "delete"), calls) }
    }

    @Test
    fun disabledActionsDoNotInvokeCallbacks() {
        var viewCalls = 0
        var editCalls = 0
        var deleteCalls = 0
        compose.setContent {
            MaterialTheme {
                EditableItemCard(
                    icon = Icons.Default.Info,
                    title = "Locked tag",
                    isEditing = false,
                    isEditEnabled = false,
                    isDeleteEnabled = false,
                    editLabel = "Edit tag",
                    deleteLabel = "Delete tag",
                    onView =
                        EditableItemViewAction(
                            onClick = { viewCalls++ },
                            contentDescription = "View targets",
                            icon = Icons.Default.LocationOn,
                            enabled = false,
                        ),
                    onStartEdit = { editCalls++ },
                    onDelete = { deleteCalls++ },
                    onEditSave = {},
                    onEditCancel = {},
                )
            }
        }

        compose.onNodeWithContentDescription("View targets").assertIsNotEnabled().performClick()
        compose.onNodeWithContentDescription("Edit tag").assertIsNotEnabled().performClick()
        compose.onNodeWithContentDescription("Delete tag").assertIsNotEnabled().performClick()
        compose.runOnIdle {
            assertEquals(0, viewCalls)
            assertEquals(0, editCalls)
            assertEquals(0, deleteCalls)
        }
    }

    @Test
    fun editModeReplacesTheCardAndForwardsSaveAndCancel() {
        var saveCalls = 0
        var cancelCalls = 0
        compose.setContent {
            MaterialTheme {
                EditableItemCard(
                    icon = Icons.Default.Info,
                    title = "Hidden while editing",
                    isEditing = true,
                    isEditEnabled = true,
                    isDeleteEnabled = true,
                    editLabel = "Edit tag",
                    deleteLabel = "Delete tag",
                    onEditSave = { saveCalls++ },
                    onEditCancel = { cancelCalls++ },
                    editContent = { onSave, onCancel ->
                        Column {
                            Button(onClick = onSave) { Text("Save changes") }
                            Button(onClick = onCancel) { Text("Cancel changes") }
                        }
                    },
                )
            }
        }

        compose.onNodeWithText("Hidden while editing").assertDoesNotExist()
        compose.onNodeWithText("Save changes").performClick()
        compose.onNodeWithText("Cancel changes").performClick()
        compose.runOnIdle {
            assertEquals(1, saveCalls)
            assertEquals(1, cancelCalls)
        }
    }
}
