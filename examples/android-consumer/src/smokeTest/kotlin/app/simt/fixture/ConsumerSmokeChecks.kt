package app.simt.fixture

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.util.Locale

@OptIn(ExperimentalTestApi::class)
abstract class ConsumerSmokeChecks {
    @get:Rule
    val compose = createComposeRule()
    private val originalLocale = Locale.getDefault()

    @After
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    private fun content(
        language: String,
        testMode: Boolean = true,
    ) {
        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)
        compose.setContent {
            val context = LocalContext.current
            val configuration = Configuration(LocalConfiguration.current).apply { setLocale(locale) }
            CompositionLocalProvider(
                LocalContext provides context.createConfigurationContext(configuration),
                LocalConfiguration provides configuration,
            ) {
                ConsumerSmoke(testMode = testMode)
            }
        }
        compose.waitForIdle()
    }

    private fun localized(
        language: String,
        clear: String,
        close: String,
        city: String,
        country: String,
    ) {
        content(language)
        // These defaults can only come from the published AAR assets: Res is internal.
        compose.onNodeWithContentDescription(clear).assertIsDisplayed()
        compose.onNodeWithContentDescription(close).assertIsDisplayed()
        compose.onNodeWithTag("field").performTextReplacement("0123456789")
        compose.onNodeWithTag("field").assert(hasText("01234"))
        compose.onNodeWithContentDescription(clear).performTouchInput { click() }
        compose.onNodeWithTag("field").assert(hasText(""))
        compose.onNodeWithContentDescription(clear).assertDoesNotExist()
        compose.onNodeWithText(city).performTextInput("x".repeat(35))
        compose.onNodeWithText(country).performTextInput("abz")
        compose.onNodeWithTag("location").assert(hasText("${"x".repeat(30)}|AB"))
        compose.onNodeWithContentDescription(close).performScrollTo().performTouchInput { click() }
        compose.onNodeWithTag("closed").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription(close).assertDoesNotExist()
    }

    @Test
    fun unsupportedLocaleFallsBackToEnglish() = localized("de", "Clear text", "Close", "City name", "Country Code (2 letters)")

    @Test
    fun defaultCountdownPreservesBothTicks() {
        compose.mainClock.autoAdvance = false
        content("en", testMode = false)
        compose.onNodeWithText("2").assertExists()
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText("2").assertExists()
        compose.onNodeWithTag("finished").assertDoesNotExist()
        compose.mainClock.advanceTimeBy(600)
        compose.onNodeWithText("1").assertExists()
        compose.onNodeWithTag("finished").assertDoesNotExist()
        compose.mainClock.advanceTimeBy(1100)
        compose.onNodeWithTag("finished").assertExists()
    }

    @Test
    fun horizontalOverflowScrollsThroughPublishedEffect() {
        content("en")
        val row = compose.onNodeWithTag("horizontal").performScrollTo()
        row.performTouchInput { swipeLeft() }
        val range = row.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.HorizontalScrollAxisRange]
        org.junit.Assert.assertTrue(range.maxValue() > 0f && range.value() > 0f)
    }

    @Test
    fun englishPackagedResourcesAndInteractions() =
        localized(
            "en",
            "Clear text",
            "Close",
            "City name",
            "Country Code (2 letters)",
        )

    @Test
    fun frenchPackagedResourcesAndInteractions() =
        localized(
            "fr",
            "Effacer le texte",
            "Fermer",
            "Nom de la ville",
            "Code pays (2 lettres)",
        )

    @Test
    fun spanishPackagedResourcesAndInteractions() =
        localized(
            "es",
            "Borrar texto",
            "Cerrar",
            "Nombre de la ciudad",
            "Código de país (2 letras)",
        )

    @Test
    fun arabicPackagedResourcesAndInteractions() =
        localized(
            "ar",
            "مسح النص",
            "إغلاق",
            "اسم المدينة",
            "رمز الدولة (حرفين)",
        )

    @Test
    fun mainActivityRendersPublishedPrimitives() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, MainActivity::class.java).putExtra("fixture.testMode", true)
        ActivityScenario.launch<MainActivity>(intent).use {
            compose.onNodeWithTag("field").assertIsDisplayed()
            compose.onNodeWithText("Synthetic header").assertIsDisplayed()
            compose.onNodeWithTag("tooltip").performScrollTo().assertIsDisplayed()
            compose.mainClock.advanceTimeBy(200)
            compose.onNodeWithTag("finished").performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun listTooltipAndCountdownInteractInConsumer() {
        content("en")
        compose.onNodeWithTag("list").performScrollTo()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(19)
        compose.onNodeWithText("Row 19").performTouchInput { click() }
        compose.onNodeWithTag("selected").assert(hasText("Selected 19"))
        compose.onNodeWithTag("tooltip").performScrollTo().performTouchInput { click() }
        compose.onNodeWithTag("clicks").assert(hasText("Clicks 1"))
        compose.onNodeWithTag("tooltip").performScrollTo().performTouchInput { longClick() }
        compose.onNodeWithText("Synthetic tooltip").assertIsDisplayed()
        compose.onNodeWithTag("clicks").assert(hasText("Clicks 1"))
        compose.mainClock.advanceTimeBy(200)
        compose.onNodeWithTag("finished").performScrollTo().assertIsDisplayed()
    }
}
