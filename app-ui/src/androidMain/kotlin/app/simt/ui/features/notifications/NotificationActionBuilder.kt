package app.simt.ui.features.notifications

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat

/**
 * Helpers for building reusable [NotificationCompat.Action]s shared across foreground
 * services.
 *
 * The dominant pattern: a foreground service exposes a single bulk action in its
 * persistent notification (e.g. "Disable all" / "Disconnect") that, when tapped,
 * delivers an [Intent] back into the same service via [PendingIntent.getService].
 * The service handles it in [Service.onStartCommand] and then [Service.stopSelf]s.
 *
 * Using one shared factory means each service only declares **what** to do (its own
 * intent action constant + label) — never the boilerplate of creating a
 * [PendingIntent] with the right flags, request code, and immutability semantics.
 */
public object NotificationActionBuilder {
    /**
     * Builds a [NotificationCompat.Action] whose tap delivers an [Intent] with the
     * given [intentAction] back into [serviceClass] via [Context.startService].
     *
     * The service must handle [intentAction] in its `onStartCommand` and perform
     * whatever bulk operation it represents (typically: disable / disconnect /
     * cancel everything, then `stopSelf()`).
     *
     * @param iconRes optional drawable shown next to the label on legacy Android
     * versions; modern OEMs ignore this for foreground-service action chips, so it
     * may be left as `0`.
     */
    public fun bulkServiceAction(
        context: Context,
        serviceClass: Class<out Service>,
        intentAction: String,
        label: CharSequence,
        @DrawableRes iconRes: Int = 0,
        operationId: String? = null,
    ): NotificationCompat.Action {
        val intent =
            Intent(context, serviceClass).apply {
                action = intentAction
                if (operationId != null) {
                    data =
                        android.net.Uri.Builder()
                            .scheme("simt")
                            .authority("service-operation")
                            .appendPath(operationId)
                            .build()
                }
            }
        val pendingIntent =
            PendingIntent.getService(
                context,
                intentAction.hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        return NotificationCompat.Action.Builder(iconRes, label, pendingIntent).build()
    }

    /**
     * Builds a [NotificationCompat.Action] whose tap delivers a broadcast [Intent] with [intentAction]
     * to [receiverClass]. Use for one-shot notification actions that should not start a service
     * (e.g. disabling a single target alert).
     *
     * @param requestCode Must be unique per distinct action instance so PendingIntents don't collide.
     */
    public fun broadcastAction(
        context: Context,
        receiverClass: Class<out BroadcastReceiver>,
        intentAction: String,
        label: CharSequence,
        requestCode: Int,
        extras: Map<String, String> = emptyMap(),
        @DrawableRes iconRes: Int = 0,
    ): NotificationCompat.Action {
        val intent =
            Intent(context, receiverClass).apply {
                action = intentAction
                extras.forEach { (key, value) -> putExtra(key, value) }
            }
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        return NotificationCompat.Action.Builder(iconRes, label, pendingIntent).build()
    }
}
