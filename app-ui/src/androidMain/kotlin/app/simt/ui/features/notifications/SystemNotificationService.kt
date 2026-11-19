package app.simt.ui.features.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService

/**
 * [NotificationService] backed by the platform [NotificationManager] / [NotificationCompat].
 *
 * Kept Hilt-free (plain constructor) so it can live in app-ui; it is provided as a singleton by
 * the application's DI graph.
 */
public class SystemNotificationService(
    private val context: Context,
) : NotificationService {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    public override fun ensureChannel(
        channelId: String,
        name: String,
        description: String?,
        importance: Int,
        showBadge: Boolean,
        soundUri: Uri?,
        vibrationPattern: LongArray?,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel =
            NotificationChannel(channelId, name, importance).apply {
                this.description = description
                setShowBadge(showBadge)
                if (vibrationPattern != null) {
                    enableVibration(true)
                    this.vibrationPattern = vibrationPattern
                }
                if (soundUri != null) {
                    val audioAttributes =
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build()
                    setSound(soundUri, audioAttributes)
                }
            }
        notificationManager.createNotificationChannel(channel)
    }

    public override fun build(spec: NotificationSpec): Notification {
        val builder =
            NotificationCompat.Builder(context, spec.channelId)
                .setContentTitle(spec.title)
                .setContentText(spec.content)
                .setSmallIcon(spec.smallIconRes)
                .setOngoing(spec.ongoing)
                .setAutoCancel(spec.autoCancel)
                .setOnlyAlertOnce(spec.onlyAlertOnce)
                .setSilent(spec.silent)
                .setShowWhen(spec.showWhen)
                .setPriority(spec.priority)
                .setVisibility(spec.visibility)

        spec.contentIntent?.let { builder.setContentIntent(it) }
        spec.category?.let { builder.setCategory(it) }
        spec.subText?.let { builder.setSubText(it) }
        spec.bigText?.let { builder.setStyle(NotificationCompat.BigTextStyle().bigText(it)) }
        spec.soundUri?.let { builder.setSound(it) }
        spec.vibrationPattern?.let { builder.setVibrate(it) }
        spec.progress?.let { builder.setProgress(it.max, it.current, it.indeterminate) }
        spec.actions.forEach { builder.addAction(it) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(spec.foregroundBehavior)
        }

        return builder.build()
    }

    public override fun notify(
        notificationId: Int,
        notification: Notification,
    ) {
        notificationManager.notify(notificationId, notification)
    }

    public override fun cancel(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    public override fun areNotificationsAllowed(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    public override fun vibrate(pattern: LongArray) {
        val vibrator =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService<VibratorManager>()?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService<Vibrator>()
            } ?: return
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }
}
