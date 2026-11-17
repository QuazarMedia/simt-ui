package app.simt.fixture

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.shared.ui.components.TopOverlayHeader
import app.shared.ui.components.WindowedList
import app.shared.ui.components.forms.CityCountryFields
import app.shared.ui.components.forms.ClearableTextField
import app.shared.ui.effects.CountdownEffect
import app.shared.ui.effects.LocalUiHapticsEnabled
import app.shared.ui.effects.LocalUiMotionEnabled
import app.shared.ui.effects.LongPressTooltipEffect
import app.shared.ui.effects.ScrollEdgeEffect
import app.shared.ui.utils.fullScreenBottomInsets
import app.shared.ui.utils.fullScreenTopInsets

// Only synthetic values. Defaults exercise published localized labels, not fixture substitutes.
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ConsumerSmoke(testMode: Boolean = false) {
    MaterialTheme {
        CompositionLocalProvider(
            LocalUiMotionEnabled provides !testMode,
            LocalUiHapticsEnabled provides !testMode,
        ) {
            var value by remember { mutableStateOf("seed") }
            var city by remember { mutableStateOf("") }
            var country by remember { mutableStateOf("") }
            var closed by remember { mutableStateOf(false) }
            var selected by remember { mutableStateOf(-1) }
            var clicks by remember { mutableStateOf(0) }
            var finished by remember { mutableStateOf(false) }
            Column(Modifier.fullScreenTopInsets().fullScreenBottomInsets().verticalScroll(rememberScrollState()).padding(8.dp)) {
                if (!closed) {
                    TopOverlayHeader("Synthetic header", onClose = { closed = true }, applyTopInsets = false)
                } else {
                    Text("Header closed", Modifier.testTag("closed"))
                }
                ClearableTextField(value, { value = it }, "Synthetic field", testTag = "field", maxLength = 5)
                CityCountryFields(city, { city = it }, country, { country = it })
                Text("$city|$country", Modifier.testTag("location"))
                WindowedList(
                    items = (0..19).toList(),
                    key = { it },
                    modifier = Modifier.fillMaxWidth().height(100.dp).testTag("list"),
                ) { _, item ->
                    Text("Row $item", Modifier.fillMaxWidth().clickable { selected = item }.padding(12.dp))
                }
                Text("Selected $selected", Modifier.testTag("selected"))
                val horizontalState = rememberScrollState()
                ScrollEdgeEffect(horizontalState, Orientation.Horizontal, Modifier.fillMaxWidth()) {
                    Row(Modifier.horizontalScroll(horizontalState).testTag("horizontal")) {
                        LongPressTooltipEffect(
                            tooltipText = "Synthetic tooltip",
                            onClick = { clicks++ },
                            fillMaxHeight = false,
                            contentModifier = Modifier.testTag("tooltip").padding(12.dp),
                        ) { Text("Tooltip anchor") }
                        Text("Horizontal end", Modifier.width(600.dp).testTag("horizontalEnd"))
                    }
                }
                Text("Clicks $clicks", Modifier.testTag("clicks"))
                if (finished) {
                    Text("Countdown finished", Modifier.testTag("finished"))
                } else {
                    CountdownEffect(
                        color = MaterialTheme.colorScheme.primary,
                        startValue = 2,
                        tickMs = if (testMode) 50 else 1000,
                        fontSize = 24.sp,
                        onFinished = { finished = true },
                    )
                }
            }
        }
    }
}
