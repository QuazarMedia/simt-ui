package app.simt.ui.features.notifications

import android.app.PendingIntent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat

/**
 * Declarative description of a single notification, decoupled from the Android builder so every
 * producer (foreground services, one-shot alerts, progress, completion) can describe *what* it
 * wants and let [NotificationService.build] handle the [NotificationCompat] boilerplate and the
 * SDK-version guards.
 *
 * Defaults intentionally mirror the [NotificationCompat.Builder] defaults so a caller only sets the
 * fields it cares about and the produced [android.app.Notification] is identical to a hand-rolled
 * builder that omitted the untouched setters.
 *
 * @property channelId Target channel; create it first with [NotificationService.ensureChannel].
 * @property smallIconRes Small status-bar icon.
 * @property ongoing Marks the notification non-dismissable (foreground-service notifications).
 * @property autoCancel Dismiss the notification when the user taps it.
 * @property onlyAlertOnce Suppress sound/vibration on subsequent updates of the same id.
 * @property silent Posts without sound/vibration (used by quiet ongoing/progress notifications).
 * @property showWhen Whether to show the timestamp row.
 * @property priority Pre-O priority (channel importance governs on O+).
 * @property category One of `NotificationCompat.CATEGORY_*` (e.g. SERVICE, ALARM).
 * @property visibility Lockscreen privacy level; private by default so sensitive content can be
 *   redacted according to the user's notification settings.
 * @property bigText When set, applies a [NotificationCompat.BigTextStyle].
 * @property subText Secondary line shown next to the app name.
 * @property progress Determinate/indeterminate progress bar.
 * @property soundUri Per-notification sound (pre-O / explicit override). Channel sound governs on O+.
 * @property vibrationPattern Per-notification vibration pattern (pre-O / explicit override).
 * @property contentIntent Tap action.
 * @property actions Inline action buttons.
 * @property foregroundBehavior `NotificationCompat.FOREGROUND_SERVICE_*` behavior (Android 12+).
 */
public data class NotificationSpec(
    public val channelId: String,
    @param:DrawableRes public val smallIconRes: Int,
    public val title: String,
    public val content: String,
    public val ongoing: Boolean = false,
    public val autoCancel: Boolean = false,
    public val onlyAlertOnce: Boolean = false,
    public val silent: Boolean = false,
    public val showWhen: Boolean = true,
    public val priority: Int = NotificationCompat.PRIORITY_DEFAULT,
    public val category: String? = null,
    public val visibility: Int = NotificationCompat.VISIBILITY_PRIVATE,
    public val bigText: String? = null,
    public val subText: String? = null,
    public val progress: Progress? = null,
    public val soundUri: Uri? = null,
    public val vibrationPattern: LongArray? = null,
    public val contentIntent: PendingIntent? = null,
    public val actions: List<NotificationCompat.Action> = emptyList(),
    public val foregroundBehavior: Int = NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE,
) {
    /** Progress-bar descriptor. [indeterminate] shows a spinner and ignores [current]. */
    public data class Progress(
        public val max: Int,
        public val current: Int,
        public val indeterminate: Boolean = false,
    )
}
