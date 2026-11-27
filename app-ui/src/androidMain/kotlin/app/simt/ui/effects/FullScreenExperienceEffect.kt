package app.simt.ui.effects

import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.simt.ui.internal.platform.findActivity
import java.util.IdentityHashMap

/**
 * Applies a full-screen experience and optionally keeps the screen awake while composed.
 *
 * Overlapping callers for the same window share ownership. Any active keep-screen-on request keeps
 * the screen on; any active hide-bars request hides the bars, with the latest active request choosing
 * gesture behavior. Each policy restores its prior state when its final owner leaves composition.
 * Applications decide which screens request this policy.
 */
@Composable
public fun FullScreenExperienceEffect(
    keepScreenOn: Boolean = true,
    hideSystemBars: Boolean = true,
    desiredSystemBarsBehavior: Int = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE,
    alwaysShowSystemBarsOnDispose: Boolean = false,
) {
    val context = LocalContext.current
    val fallbackView = LocalView.current
    val activity = context.findActivity()
    val dialogWindow = (fallbackView.parent as? DialogWindowProvider)?.window
    val window = dialogWindow ?: activity?.window
    val targetView = window?.decorView ?: fallbackView

    DisposableEffect(
        activity,
        targetView,
        keepScreenOn,
        hideSystemBars,
        desiredSystemBarsBehavior,
        alwaysShowSystemBarsOnDispose,
    ) {
        val owner =
            fullscreenPolicyCoordinator.acquire(
                targetView = targetView,
                window = window,
                keepScreenOn = keepScreenOn,
                hideSystemBars = hideSystemBars,
                desiredSystemBarsBehavior = desiredSystemBarsBehavior,
                alwaysShowSystemBarsOnDispose = alwaysShowSystemBarsOnDispose,
            )
        onDispose { fullscreenPolicyCoordinator.release(targetView, owner) }
    }
}

internal class FullscreenPolicyCoordinator {
    private data class Request(
        val keepScreenOn: Boolean,
        val hideSystemBars: Boolean,
        val desiredSystemBarsBehavior: Int,
        val alwaysShowSystemBarsOnDispose: Boolean,
    )

    private data class Owner(
        val token: Any,
        val request: Request,
    )

    private data class Session(
        val targetView: View,
        val window: Window?,
        val controller: WindowInsetsControllerCompat?,
        val owners: MutableList<Owner> = mutableListOf(),
        var initialKeepScreenOnView: Boolean? = null,
        var initialKeepScreenOnFlagSet: Boolean = false,
        var initialSystemBars: SystemBarVisibility? = null,
        var initialSystemBarsBehavior: Int? = null,
        var keepScreenOnActive: Boolean = false,
        var systemBarsActive: Boolean = false,
    )

    private val sessions = IdentityHashMap<View, Session>()

    @Synchronized
    fun acquire(
        targetView: View,
        window: Window?,
        keepScreenOn: Boolean,
        hideSystemBars: Boolean,
        desiredSystemBarsBehavior: Int,
        alwaysShowSystemBarsOnDispose: Boolean,
    ): Any {
        val session =
            sessions.getOrPut(targetView) {
                Session(
                    targetView = targetView,
                    window = window,
                    controller = window?.let { WindowInsetsControllerCompat(it, targetView) },
                )
            }
        if (keepScreenOn && session.initialKeepScreenOnView == null) {
            session.initialKeepScreenOnView = targetView.keepScreenOn
            session.initialKeepScreenOnFlagSet =
                window?.attributes
                    ?.flags
                    ?.and(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    ?.let { it != 0 }
                    ?: false
        }
        if (hideSystemBars && session.initialSystemBars == null) {
            session.initialSystemBars =
                ViewCompat.getRootWindowInsets(targetView)
                    ?.let { insets ->
                        SystemBarVisibility(
                            statusBars = insets.isVisible(WindowInsetsCompat.Type.statusBars()),
                            navigationBars = insets.isVisible(WindowInsetsCompat.Type.navigationBars()),
                        )
                    }
                    ?: SystemBarVisibility()
            session.initialSystemBarsBehavior = session.controller?.systemBarsBehavior
        }

        val token = Any()
        session.owners +=
            Owner(
                token = token,
                request =
                    Request(
                        keepScreenOn = keepScreenOn,
                        hideSystemBars = hideSystemBars,
                        desiredSystemBarsBehavior = desiredSystemBarsBehavior,
                        alwaysShowSystemBarsOnDispose = alwaysShowSystemBarsOnDispose,
                    ),
            )
        apply(session)
        return token
    }

    @Synchronized
    fun release(
        targetView: View,
        token: Any,
    ) {
        val session = sessions[targetView] ?: return
        val index = session.owners.indexOfFirst { it.token == token }
        if (index < 0) return

        val removed = session.owners.removeAt(index)
        if (session.owners.isEmpty()) sessions.remove(targetView)
        apply(
            session = session,
            alwaysShowSystemBars =
                removed.request.hideSystemBars && removed.request.alwaysShowSystemBarsOnDispose,
        )
    }

    private fun apply(
        session: Session,
        alwaysShowSystemBars: Boolean = false,
    ) {
        val keepScreenOn = session.owners.any { it.request.keepScreenOn }
        if (keepScreenOn) {
            if (!session.keepScreenOnActive) {
                session.targetView.keepScreenOn = true
                if (session.window != null && !session.initialKeepScreenOnFlagSet) {
                    session.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                session.keepScreenOnActive = true
            }
        } else if (session.keepScreenOnActive) {
            session.targetView.keepScreenOn = session.initialKeepScreenOnView ?: false
            if (session.window != null && !session.initialKeepScreenOnFlagSet) {
                session.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            session.keepScreenOnActive = false
            session.initialKeepScreenOnView = null
            session.initialKeepScreenOnFlagSet = false
        }

        val barsRequest = session.owners.lastOrNull { it.request.hideSystemBars }
        val controller = session.controller
        if (barsRequest != null && controller != null) {
            controller.systemBarsBehavior = barsRequest.request.desiredSystemBarsBehavior
            controller.hide(WindowInsetsCompat.Type.systemBars())
            session.systemBarsActive = true
        } else if (session.systemBarsActive && controller != null) {
            val initialSystemBars = session.initialSystemBars ?: SystemBarVisibility()
            val initialSystemBarsBehavior = session.initialSystemBarsBehavior
            if (initialSystemBarsBehavior != null) {
                controller.systemBarsBehavior = initialSystemBarsBehavior
            }
            if (alwaysShowSystemBars || initialSystemBars.statusBars) {
                controller.show(WindowInsetsCompat.Type.statusBars())
            } else {
                controller.hide(WindowInsetsCompat.Type.statusBars())
            }
            if (alwaysShowSystemBars || initialSystemBars.navigationBars) {
                controller.show(WindowInsetsCompat.Type.navigationBars())
            } else {
                controller.hide(WindowInsetsCompat.Type.navigationBars())
            }
            session.systemBarsActive = false
            session.initialSystemBars = null
            session.initialSystemBarsBehavior = null
        }
    }
}

private data class SystemBarVisibility(
    val statusBars: Boolean = true,
    val navigationBars: Boolean = true,
)

private val fullscreenPolicyCoordinator = FullscreenPolicyCoordinator()
