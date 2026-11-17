package app.simt.ui.preview

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.simt.ui.components.TopOverlayHeader
import app.simt.ui.components.TopOverlayToolToggle
import app.simt.ui.components.WindowedList
import app.simt.ui.components.forms.CityCountryFields
import app.simt.ui.components.forms.ClearableTextField
import app.simt.ui.effects.CountdownEffect
import app.simt.ui.effects.LocalUiHapticsEnabled
import app.simt.ui.effects.LocalUiMotionEnabled
import app.simt.ui.effects.LongPressTooltipEffect
import app.simt.ui.effects.ScrollEdgeEffect
import app.simt.ui.utils.fullScreenBottomInsets
import app.simt.ui.utils.fullScreenTopInsets

private val PreviewIcon =
    ImageVector.Builder("Synthetic diamond", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 3f)
            lineTo(21f, 12f)
            lineTo(12f, 21f)
            lineTo(3f, 12f)
            close()
        }
    }.build()

@Composable
private fun PreviewSurface(
    height: Dp = 320.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        CompositionLocalProvider(LocalUiMotionEnabled provides false, LocalUiHapticsEnabled provides false) {
            Surface(Modifier.fillMaxWidth().height(height)) {
                Column(
                    Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content,
                )
            }
        }
    }
}

@Preview(name = "Fields · light", group = "Forms", widthDp = 380, heightDp = 560)
@Preview(name = "Fields · dark / large text", group = "Forms", widthDp = 420, heightDp = 680, fontScale = 1.3f, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun ClearableTextFieldPreview() {
    PreviewSurface(height = 560.dp) {
        var populated by remember { mutableStateOf("Synthetic value") }
        ClearableTextField("", {}, "Empty field", placeholder = "Enter a sample", testTag = "empty-field")
        ClearableTextField(populated, { populated = it }, "Populated field", testTag = "populated-field")
        ClearableTextField("Invalid sample", {}, "Error field", isError = true, supportingText = { Text("Sample validation error") }, testTag = "error-field")
        ClearableTextField("Supported sample", {}, "Supporting field", supportingText = { Text("Synthetic supporting text") })
        ClearableTextField("Read-only sample", {}, "Read-only field", readOnly = true, testTag = "readonly-field")
    }
}

@Preview(name = "Editable location", group = "Forms", widthDp = 380)
@Preview(name = "Location · French", group = "Forms", widthDp = 380, locale = "fr")
@Composable
internal fun CityCountryFieldsPreview() {
    PreviewSurface(height = 240.dp) {
        var city by remember { mutableStateOf("Sampleville") }
        var country by remember { mutableStateOf("FR") }
        CityCountryFields(city, { city = it }, country, { country = it }, Modifier.fillMaxWidth())
    }
}

private data class SampleRow(val id: Int, val label: String)

@Preview(name = "Typed list · middle / overflow", group = "Lists", widthDp = 380)
@Composable
internal fun WindowedListPreview() {
    PreviewSurface {
        val rows = remember { List(30) { SampleRow(it, "Sample row $it") } }
        WindowedList(
            items = rows,
            key = { it.id },
            contentType = { "sample-row" },
            state = rememberLazyListState(initialFirstVisibleItemIndex = 10),
            modifier = Modifier.fillMaxWidth().height(240.dp).testTag("typed-list"),
        ) { _, row ->
            Text(row.label, Modifier.fillMaxWidth().padding(16.dp))
        }
    }
}

@Preview(name = "Empty list", group = "Lists", widthDp = 380)
@Composable
internal fun WindowedListEmptyPreview() {
    PreviewSurface {
        WindowedList(
            items = emptyList<SampleRow>(),
            key = { it.id },
            modifier = Modifier.fillMaxWidth().height(240.dp),
            emptyContent = { Text("No synthetic rows") },
        ) { _, row -> Text(row.label) }
    }
}

@Preview(name = "List DSL overload", group = "Lists", widthDp = 380)
@Composable
internal fun WindowedListDslPreview() {
    PreviewSurface {
        WindowedList(Modifier.fillMaxWidth().height(240.dp).testTag("dsl-list")) {
            item(key = "section") { Text("Synthetic section", Modifier.padding(16.dp)) }
            items(count = 20, key = { "dsl-$it" }) { index ->
                Text("DSL row $index", Modifier.fillMaxWidth().padding(16.dp))
            }
        }
    }
}

@Preview(name = "Attached scroll edges · both axes", group = "Effects", widthDp = 380, heightDp = 400)
@Preview(name = "Scroll edges · RTL", group = "Effects", widthDp = 380, heightDp = 400, locale = "ar")
@Composable
internal fun ScrollEdgeEffectPreview() {
    PreviewSurface(height = 400.dp) {
        val horizontal = rememberScrollState(initial = 200)
        val vertical = rememberScrollState(initial = 180)
        Text("Horizontal overflow")
        ScrollEdgeEffect(horizontal, Orientation.Horizontal, Modifier.fillMaxWidth().height(72.dp)) {
            Row(Modifier.fillMaxSize().horizontalScroll(horizontal).testTag("horizontal-scroll")) {
                repeat(12) { Text("Tile $it", Modifier.width(120.dp).padding(16.dp)) }
            }
        }
        Text("Vertical overflow")
        ScrollEdgeEffect(vertical, Orientation.Vertical, Modifier.fillMaxWidth().height(180.dp)) {
            Column(Modifier.fillMaxSize().verticalScroll(vertical).testTag("vertical-scroll")) {
                repeat(20) { Text("Vertical row $it", Modifier.fillMaxWidth().padding(16.dp)) }
            }
        }
    }
}

@Preview(name = "Tooltip anchor · interactive long press", group = "Effects", widthDp = 380)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LongPressTooltipEffectPreview() {
    PreviewSurface(height = 180.dp) {
        LongPressTooltipEffect(
            tooltipText = "Synthetic tooltip help",
            fillMaxHeight = false,
            contentModifier = Modifier.testTag("tooltip-anchor").padding(16.dp),
        ) { Text("Long press for sample help") }
    }
}

@Preview(name = "Header · portrait", group = "Header", widthDp = 412, heightDp = 240)
@Preview(name = "Header · landscape / dark", group = "Header", widthDp = 800, heightDp = 240, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun TopOverlayHeaderPreview() {
    PreviewSurface(height = 240.dp) {
        var selected by remember { mutableStateOf(true) }
        var unselected by remember { mutableStateOf(false) }
        TopOverlayHeader(
            title = "Survey",
            subtitle = "Synthetic session",
            onClose = {},
            applyTopInsets = false,
            icon = { Icon(PreviewIcon, contentDescription = null) },
            tools =
                listOf(
                    TopOverlayToolToggle(selected, PreviewIcon, onClick = { selected = !it }, contentDescription = "Selected tool"),
                    TopOverlayToolToggle(unselected, PreviewIcon, onClick = { unselected = !it }, contentDescription = "Unselected tool"),
                    TopOverlayToolToggle(false, PreviewIcon, onClick = {}, contentDescription = "Disabled tool", disabled = true),
                ),
            contentCard = { Text("Synthetic header card") },
        )
    }
}

@Preview(name = "Countdown · stable one / reduced motion", group = "Effects", widthDp = 380)
@Composable
internal fun CountdownEffectPreview() {
    PreviewSurface {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CountdownEffect(
                color = MaterialTheme.colorScheme.primary,
                startValue = 1,
                tickMs = 0,
                onStart = null,
                onFinished = {},
                subtitle = "Synthetic countdown",
                subtitleColor = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview(name = "Fullscreen inset sample · system UI", group = "Insets", widthDp = 412, heightDp = 640, showSystemUi = true)
@Composable
internal fun FullscreenInsetsPreview() {
    PreviewSurface(height = 560.dp) {
        Column(Modifier.fillMaxSize().fullScreenTopInsets().fullScreenBottomInsets(), verticalArrangement = Arrangement.SpaceBetween) {
            Text("Top inset sample")
            Text("Bottom inset sample")
        }
    }
}
