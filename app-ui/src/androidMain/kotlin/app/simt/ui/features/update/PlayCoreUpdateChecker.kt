package app.simt.ui.features.update

import android.app.Activity
import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * [UpdateChecker] backed by Google Play in-app updates.
 *
 * Construct with the application context; the underlying [AppUpdateManager]
 * is created lazily and reused for the process lifetime. [checkForUpdate]
 * never throws: when the Play service is unavailable (sideloaded
 * installation, store outage) it reports [UpdateCheckResult.Unavailable].
 */
public class PlayCoreUpdateChecker(context: Context) : UpdateChecker {
    private val appUpdateManager: AppUpdateManager =
        AppUpdateManagerFactory.create(context.applicationContext)

    public override suspend fun checkForUpdate(): UpdateCheckResult =
        withContext(Dispatchers.IO) {
            runCatching { appUpdateManager.appUpdateInfo.await() }.fold(
                onSuccess = { info ->
                    val available =
                        info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                            info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                    if (available) UpdateCheckResult.Available else UpdateCheckResult.Unavailable
                },
                onFailure = { UpdateCheckResult.Unavailable },
            )
        }

    public override fun startUpdateFlow(activityProvider: () -> Any?) {
        val activity = activityProvider() as? Activity ?: return
        runCatching {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
                if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                ) {
                    appUpdateManager.startUpdateFlowForResult(
                        info,
                        activity,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                        UPDATE_REQUEST_CODE,
                    )
                }
            }
        }
    }

    private companion object {
        const val UPDATE_REQUEST_CODE = 0x0220A
    }
}
