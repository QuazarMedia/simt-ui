export default {
  id: 'app-ui',
  title: 'Native UI catalog',
  description:
    'Shared Android and iOS Compose UI primitives, inspected through real Android host captures, production Kotlin source and published ABI dumps.',
  metadata: {
    name: 'app.simt:app-ui',
    description: 'Shared Android and iOS Compose UI primitives',
    versionFile: '../gradle.properties',
    versionProperty: 'VERSION_NAME',
    platforms: ['Android · API 28+', 'iOS · iosArm64', 'iOS Simulator · iosSimulatorArm64'],
    sourceRoots: ['commonMain/kotlin', 'androidMain/kotlin', 'iosMain/kotlin'],
  },
  usage: `dependencies {
    implementation("app.simt:app-ui:{{version}}")
}

// The caller owns the Material3 theme, state, labels,
// callbacks, window policy and lifecycle.`,
  evidence: {
    capture: {
      label: 'Robolectric Android host capture',
      environment: 'Android API 36 · 420 × 800 dp · mdpi · English · caller Material3 light theme',
      behavior: 'Synthetic fixture content with decorative motion and haptics disabled.',
      command:
        './gradlew :app-ui:publishAndroidPublicationToLocalValidationRepository && ./gradlew -p examples/android-consumer -PuiRepository=../../build/maven-repository -PpreviewOutput=../app-ui/previews testDebugUnitTest --tests app.simt.ui.preview.SharedUiPreviewsTest',
      sources: [
        '../examples/android-consumer/src/debug/kotlin/app/simt/ui/preview/SharedUiPreviews.kt',
        '../examples/android-consumer/src/test/kotlin/app/simt/ui/preview/SharedUiPreviewsTest.kt',
      ],
    },
    limits: [
      'These are static Android host captures, not an interactive browser preview and not emulator or physical-device evidence.',
      'The capture harness exercises Android API 36. Android API 28 remains supported by the library but is host-only in the recorded compatibility evidence.',
      'iOS source and klib ABI are published here. iOS launch, bundled-resource rendering, native IME, screen-reader behavior and physical haptics remain unvalidated.',
      'A pure Swift or SwiftUI API is not promised; consumers use the Kotlin Multiplatform Compose API.',
    ],
  },
  components: [
    {
      id: 'fields',
      title: 'Clearable text fields',
      description: 'Empty, populated, error, supporting-text and read-only states from one caller-state-owned field.',
      capture: 'fields.png',
      source: ['src/commonMain/kotlin/app/simt/ui/components/forms/ClearableTextField.kt'],
      contracts: ['Caller-owned value', 'Localized clear action', 'Error and supporting-text slots'],
      usage: `var value by remember { mutableStateOf("Sample") }

ClearableTextField(
    value = value,
    onValueChange = { value = it },
    label = "Sample field",
    supportingText = { Text("Caller-owned guidance") },
)`,
    },
    {
      id: 'location',
      title: 'City and country fields',
      description: 'A paired location input with normalized country-code presentation.',
      capture: 'location.png',
      source: ['src/commonMain/kotlin/app/simt/ui/components/forms/CityCountryFields.kt'],
      contracts: ['Caller-owned city and country', 'Two-letter country normalization', 'Shared field styling'],
      usage: `CityCountryFields(
    city = city,
    onCityChange = { city = it },
    countryCode = countryCode,
    onCountryCodeChange = { countryCode = it },
)`,
    },
    {
      id: 'typed-list',
      title: 'Windowed typed list',
      description: 'A keyed lazy list with stable content types and an explicitly controlled initial scroll position.',
      capture: 'typed-list.png',
      source: ['src/commonMain/kotlin/app/simt/ui/components/WindowedList.kt'],
      contracts: ['Stable keys', 'Content types', 'Caller-provided LazyListState'],
      usage: `WindowedList(
    items = rows,
    key = Row::id,
    contentType = { "row" },
    state = rememberLazyListState(initialFirstVisibleItemIndex = 10),
) { _, row ->
    Text(row.label)
}`,
    },
    {
      id: 'empty-list',
      title: 'Empty list state',
      description: 'The same list primitive with a caller-authored empty state instead of fabricated rows.',
      capture: 'empty-list.png',
      source: ['src/commonMain/kotlin/app/simt/ui/components/WindowedList.kt'],
      contracts: ['Typed empty state', 'No implicit placeholder data', 'Shared list behavior'],
      usage: `WindowedList(
    items = emptyList<Row>(),
    key = Row::id,
    emptyContent = { Text("No rows yet") },
) { _, row ->
    Text(row.label)
}`,
    },
    {
      id: 'dsl-list',
      title: 'List DSL overload',
      description: 'Section headers and indexed rows through the lazy-list DSL without a separate component implementation.',
      capture: 'dsl-list.png',
      source: ['src/commonMain/kotlin/app/simt/ui/components/WindowedList.kt'],
      contracts: ['LazyListScope', 'Stable section and row keys', 'Single windowing implementation'],
      usage: `WindowedList {
    item(key = "section") { Text("Section") }
    items(count = rows.size, key = { rows[it].id }) { index ->
        Text(rows[index].label)
    }
}`,
    },
    {
      id: 'scroll-edges',
      title: 'Attached scroll edges',
      description: 'Horizontal and vertical overflow indicators attached to real scroll state.',
      capture: 'scroll-edges.png',
      source: ['src/commonMain/kotlin/app/simt/ui/effects/ScrollEdgeEffect.kt'],
      contracts: ['ScrollState-driven visibility', 'Horizontal and vertical orientation', 'Reduced-motion policy'],
      usage: `val state = rememberScrollState()

ScrollEdgeEffect(state, Orientation.Vertical) {
    Column(Modifier.verticalScroll(state)) {
        // Caller-owned scrollable content.
    }
}`,
    },
    {
      id: 'tooltip',
      title: 'Long-press tooltip',
      description: 'A tooltip effect whose anchor, copy and interaction remain owned by the caller.',
      capture: 'tooltip.png',
      source: ['src/commonMain/kotlin/app/simt/ui/effects/LongPressTooltipEffect.kt'],
      contracts: ['Caller-provided tooltip copy', 'Long-press interaction', 'Caller-owned anchor modifier'],
      usage: `LongPressTooltipEffect(tooltipText = "More information") {
    Text("Press and hold for help")
}`,
    },
    {
      id: 'header',
      title: 'Top overlay header',
      description: 'A bounded overlay header with selected, unselected and disabled tool toggles.',
      capture: 'header.png',
      source: ['src/commonMain/kotlin/app/simt/ui/components/TopOverlayHeader.kt'],
      contracts: ['Caller-owned selection', 'Selected and disabled semantics', 'Optional top-inset application'],
      usage: `TopOverlayHeader(
    title = "Survey",
    subtitle = "Current session",
    onClose = onClose,
    applyTopInsets = false,
    tools = tools,
    contentCard = { Text("Session details") },
)`,
    },
    {
      id: 'countdown',
      title: 'Stable countdown',
      description: 'A one-second countdown fixture with motion disabled and no recurring timer work.',
      capture: 'countdown.png',
      source: ['src/commonMain/kotlin/app/simt/ui/effects/CountdownEffect.kt'],
      contracts: ['Caller-owned completion callback', 'Stable displayed value', 'Motion and haptics policy'],
      usage: `CountdownEffect(
    color = MaterialTheme.colorScheme.primary,
    startValue = 3,
    tickMs = 1_000,
    onStart = onStart,
    onFinished = onFinished,
    subtitle = "Get ready",
)`,
    },
    {
      id: 'insets',
      title: 'Fullscreen insets',
      description: 'Top and bottom inset modifiers applied to caller-owned fullscreen content.',
      capture: 'insets.png',
      source: ['src/commonMain/kotlin/app/simt/ui/utils/FullscreenInsets.kt'],
      contracts: ['Platform inset resolution', 'Top and bottom modifiers', 'Caller-owned window policy'],
      usage: `Column(
    Modifier
        .fillMaxSize()
        .fullScreenTopInsets()
        .fullScreenBottomInsets()
) {
    // Caller-owned fullscreen content.
}`,
    },
  ],
};
