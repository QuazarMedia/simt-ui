package app.simt.ui.features.notifications

import android.app.Notification
import android.app.NotificationManager
import android.net.Uri

/**
 * App-wide facade for system notifications. Centralizes channel creation, notification building,
 * posting/cancelling, the runtime POST_NOTIFICATIONS check, and vibration so every feature shares
 * one implementation instead of hand-rolling [NotificationManager] / `NotificationCompat` calls.
 *
 * Foreground services keep calling `startForeground(...)` themselves with the [Notification]
 * returned by [build]; this service never promotes a service to the foreground.
 */
public interface NotificationService {
    /**
     * Creates (or updates) a notification channel. No-op below Android O.
     *
     * @param channelId Stable channel id.
     * @param name User-visible channel name.
     * @param description User-visible channel description.
     * @param importance One of `NotificationManager.IMPORTANCE_*` (default LOW: no sound).
     * @param showBadge Whether the launcher may show a badge for this channel.
     * @param soundUri Channel sound; when set, played with a notification audio attribute.
     * @param vibrationPattern Channel vibration pattern; when set, vibration is enabled.
     */
    public fun ensureChannel(
        channelId: String,
        name: String,
        description: String? = null,
        importance: Int = NotificationManager.IMPORTANCE_LOW,
        showBadge: Boolean = true,
        soundUri: Uri? = null,
        vibrationPattern: LongArray? = null,
    ): Unit

    /** Builds a [Notification] from [spec] applying the relevant SDK-version guards. */
    public fun build(spec: NotificationSpec): Notification

    /** Posts or updates the notification with the given [notificationId]. */
    public fun notify(
        notificationId: Int,
        notification: Notification,
    ): Unit

    /** Cancels the notification with the given [notificationId]. */
    public fun cancel(notificationId: Int): Unit

    /**
     * Whether the app may post notifications: the runtime POST_NOTIFICATIONS grant on Android 13+,
     * always `true` below it.
     */
    public fun areNotificationsAllowed(): Boolean

    /** Fires a one-shot vibration [pattern] (no-op when no vibrator is present). */
    public fun vibrate(pattern: LongArray): Unit
}
