package app.simt.ui.components.forms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.simt.ui.components.buttons.DialogActionButtons
import app.simt.ui.components.dialogs.ContentDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun TimePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    testTag: String? = null,
) {
    var showDialog by remember { mutableStateOf(false) }

    val (initialHour, initialMinute) =
        remember(value) {
            if (value.contains(":")) {
                val parts = value.split(":")
                Pair(parts[0].toIntOrNull() ?: 0, parts[1].toIntOrNull() ?: 0)
            } else {
                Pair(0, 0)
            }
        }

    ClearableTextField(
        value = if (value.isNotBlank()) value else "",
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        readOnly = true,
        onClick = { showDialog = true },
        leadingIcon = leadingIcon,
        maxLength = null,
        testTag = testTag,
    )

    if (showDialog) {
        val timePickerState =
            rememberTimePickerState(
                initialHour = initialHour,
                initialMinute = initialMinute,
                is24Hour = true,
            )

        ContentDialog(onDismissRequest = { showDialog = false }) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TimePicker(state = timePickerState)

                DialogActionButtons(
                    onCancel = { showDialog = false },
                    onSave = {
                        val h = timePickerState.hour.toString().padStart(2, '0')
                        val m = timePickerState.minute.toString().padStart(2, '0')
                        onValueChange("$h:$m")
                        showDialog = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
