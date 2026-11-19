package app.simt.ui.features.notifications

import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.getSystemService
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Smoke coverage for the notifications kit: channel creation, spec-to-notification mapping,
 * post/cancel, and the two action builders.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NotificationKitTest {
    private lateinit var context: Context
    private lateinit var service: SystemNotificationService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        service = SystemNotificationService(context)
    }

    @Test
    fun `ensureChannel creates a channel with the requested attributes`() {
        service.ensureChannel(
            channelId = "alerts",
            name = "Alerts",
            description = "Target alerts",
            importance = android.app.NotificationManager.IMPORTANCE_HIGH,
            showBadge = false,
            vibrationPattern = longArrayOf(0, 300, 200, 300),
        )

        val channel = context.getSystemService<android.app.NotificationManager>()?.getNotificationChannel("alerts")
        assertNotNull("the channel must exist after ensureChannel", channel)
        assertEquals(android.app.NotificationManager.IMPORTANCE_HIGH, channel!!.importance)
        assertEquals("Target alerts", channel.description)
        assertEquals(false, channel.canShowBadge())
        assertEquals(4, channel.vibrationPattern?.size ?: 0)
    }

    @Test
    fun `build maps the spec onto the notification`() {
        val notification =
            service.build(
                NotificationSpec(
                    channelId = "alerts",
                    smallIconRes = 0,
                    title = "Backup running",
                    content = "Uploading 3 targets",
                    ongoing = true,
                    autoCancel = true,
                    silent = true,
                ),
            )

        assertEquals("alerts", notification.channelId)
        assertEquals(true, (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0)
        assertEquals(true, (notification.flags and Notification.FLAG_AUTO_CANCEL) != 0)
        assertEquals("Backup running", notification.extras.getCharSequence(Notification.EXTRA_TITLE))
        assertEquals("Uploading 3 targets", notification.extras.getCharSequence(Notification.EXTRA_TEXT))
    }

    @Test
    fun `notify posts and cancel removes the notification`() {
        val notification =
            service.build(
                NotificationSpec(
                    channelId = "alerts",
                    smallIconRes = 0,
                    title = "T",
                    content = "C",
                ),
            )

        service.notify(42, notification)
        val manager = context.getSystemService<android.app.NotificationManager>()!!
        assertEquals(1, manager.activeNotifications.size)
        assertEquals(42, manager.activeNotifications[0].id)

        service.cancel(42)
        assertEquals(0, manager.activeNotifications.size)
    }

    @Test
    fun `bulkServiceAction delivers its intent back into the service`() {
        val action =
            NotificationActionBuilder.bulkServiceAction(
                context = context,
                serviceClass = TestService::class.java,
                intentAction = "app.simt.test.DISABLE_ALL",
                label = "Disable all",
            )

        assertEquals("Disable all", action.title.toString())
        val intent = shadowOf(action.actionIntent!!).savedIntent
        assertNotNull("the action must carry a pending intent", intent)
        assertEquals("app.simt.test.DISABLE_ALL", intent?.action)
        assertEquals(TestService::class.java.name, intent?.component?.className)
    }

    @Test
    fun `broadcastAction carries the extras into its broadcast`() {
        val action =
            NotificationActionBuilder.broadcastAction(
                context = context,
                receiverClass = TestReceiver::class.java,
                intentAction = "app.simt.test.DISABLE_ONE",
                label = "Disable",
                requestCode = 7,
                extras = mapOf("targetId" to "t-1"),
            )

        assertEquals("Disable", action.title.toString())
        val intent = shadowOf(action.actionIntent!!).savedIntent
        assertNotNull("the action must carry a pending intent", intent)
        assertEquals("app.simt.test.DISABLE_ONE", intent?.action)
        assertEquals("t-1", intent?.getStringExtra("targetId"))
    }

    private class TestService : Service() {
        override fun onBind(intent: Intent): IBinder? = null
    }

    private class TestReceiver : BroadcastReceiver() {
        override fun onReceive(
            context: Context?,
            intent: Intent?,
        ) {}
    }
}
