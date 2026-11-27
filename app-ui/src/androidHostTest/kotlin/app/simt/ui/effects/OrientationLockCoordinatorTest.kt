package app.simt.ui.effects

import android.content.pm.ActivityInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class OrientationLockCoordinatorTest {
    @Test
    fun `first owner locks and final owner restores the previous policy`() {
        val target = FakeOrientationTarget(orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
        val coordinator = coordinator()

        coordinator.acquire(target)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LOCKED, target.orientation)

        coordinator.release(target)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, target.orientation)
    }

    @Test
    fun `overlapping owners keep orientation locked until both release`() {
        val target = FakeOrientationTarget(orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT)
        val coordinator = coordinator()

        coordinator.acquire(target)
        coordinator.acquire(target)
        coordinator.release(target)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LOCKED, target.orientation)

        coordinator.release(target)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT, target.orientation)
    }

    @Test
    fun `different activity instances restore independently`() {
        val portraitTarget = FakeOrientationTarget(orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT)
        val landscapeTarget = FakeOrientationTarget(orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
        val coordinator = coordinator()

        coordinator.acquire(portraitTarget)
        coordinator.acquire(landscapeTarget)
        coordinator.release(portraitTarget)

        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT, portraitTarget.orientation)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LOCKED, landscapeTarget.orientation)

        coordinator.release(landscapeTarget)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, landscapeTarget.orientation)
    }

    @Test
    fun `extra release is ignored without changing orientation`() {
        val target = FakeOrientationTarget(orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
        val coordinator = coordinator()

        coordinator.release(target)

        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, target.orientation)
    }

    private fun coordinator() =
        OrientationLockCoordinator<FakeOrientationTarget>(
            lockedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED,
            readOrientation = { it.orientation },
            writeOrientation = { target, orientation -> target.orientation = orientation },
        )

    private data class FakeOrientationTarget(var orientation: Int)
}
