package app.simt.ui.components.forms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
public fun RadioOptionLine(
    selected: Boolean,
    label: String,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    contentPadding: PaddingValues = PaddingValues(start = 12.dp, end = 12.dp, bottom = 8.dp),
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onSelect() },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val radioModifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
            RadioButton(
                selected = selected,
                onClick = onSelect,
                modifier = radioModifier,
            )
            Text(text = label)
        }

        AnimatedVisibility(visible = selected) {
            Column(modifier = Modifier.padding(contentPadding)) {
                content()
            }
        }
    }
}
