package app.simt.ui.effects

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import app.simt.ui.transitions.appScreenEnterTransition
import app.simt.ui.transitions.appScreenExitTransition
import app.simt.ui.transitions.appScreenTransition
import app.simt.ui.transitions.staggeredEntrance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MotionPrimitivesTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun screenTransitionCombinesSharedEnterAndExit() {
        val transition = appScreenTransition()

        assertEquals(appScreenEnterTransition(), transition.targetContentEnter)
        assertEquals(appScreenExitTransition(), transition.initialContentExit)
    }

    @Test
    fun staggeredEntranceAnimatesToItsFinalPosition() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                MotionTestItem()
            }
        }

        compose.mainClock.advanceTimeByFrame()
        val initialTop = compose.onNodeWithTag("motion-item").getUnclippedBoundsInRoot().top
        compose.mainClock.advanceTimeBy(500)
        val finalTop = compose.onNodeWithTag("motion-item").getUnclippedBoundsInRoot().top

        assertTrue("initial=$initialTop final=$finalTop", initialTop > finalTop + 20.dp)
    }

    @Test
    fun staggeredEntranceSkipsAnimationWhenMotionIsDisabled() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalUiMotionEnabled provides false) {
                MaterialTheme {
                    MotionTestItem()
                }
            }
        }

        compose.mainClock.advanceTimeByFrame()
        val top = compose.onNodeWithTag("motion-item").getUnclippedBoundsInRoot().top

        assertEquals(0f, top.value, 1f)
    }

    @Test
    fun staggeredEntranceSkipsAnimationOnStaticTier() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalCosmosTier provides CosmosTier.STATIC) {
                MaterialTheme {
                    MotionTestItem()
                }
            }
        }

        compose.mainClock.advanceTimeByFrame()
        val top = compose.onNodeWithTag("motion-item").getUnclippedBoundsInRoot().top

        assertEquals(0f, top.value, 1f)
    }

    @Composable
    private fun MotionTestItem() {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .size(20.dp)
                        .staggeredEntrance(delayMs = 0)
                        .testTag("motion-item"),
            )
        }
    }
}
