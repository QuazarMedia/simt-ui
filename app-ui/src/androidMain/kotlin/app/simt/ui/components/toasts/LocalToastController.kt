package app.simt.ui.components.toasts

import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.flow.StateFlow

/** Visual style of a toast emitted through [ToastController]. */
public enum class ToastStyle {
    SUCCESS,
    ERROR,
    INFO,
}

/**
 * One toast in the stack that [GlobalToastHost] is currently rendering.
 *
 * [id] is monotonic and unique per emission so the host can key each stacked entry independently —
 * this keeps every toast's enter/exit animation and auto-dismiss timer scoped to its own slot, even
 * when two toasts carry the same [message].
 */
@Immutable
public data class ToastUiState(
    public val id: Long,
    public val message: String,
    public val style: ToastStyle,
    /** Custom leading icon. Only used by [ToastStyle.INFO]; success/error use their fixed icons. */
    public val icon: ImageVector? = null,
)

/**
 * Application-wide controller for the global toast banner overlay.
 *
 * Designed to be injected like [app.simt.ui.components.loader.BlockingLoaderController] and consumed
 * from anywhere — view-models, navigation graphs, screens — so that one-shot success / error
 * messages render above whichever screen happens to be on top, rather than being tied to a single
 * feature's renderer.
 *
 * The controller holds a stack of toasts: emitting a new one appends it below the others so several
 * messages can be shown at once. Each entry animates in and auto-dismisses independently via
 * [GlobalToastHost].
 */
public interface ToastController {
    public val uiState: StateFlow<List<ToastUiState>>

    /** Show a success (green check) toast with [message]. Added to the bottom of the toast stack. */
    public fun showSuccess(message: String)

    /** Show an error (red warning) toast with [message]. Added to the bottom of the toast stack. */
    public fun showError(message: String)

    /**
     * Show an informational toast with [message] and a custom leading [icon]. Added to the bottom of
     * the toast stack. Used for transient notifications that should appear over any screen via the
     * global toast host.
     */
    public fun showInfo(
        message: String,
        icon: ImageVector,
    )

    /** Dismiss the toast identified by [id]. No-op when no toast with that id is in the stack. */
    public fun dismiss(id: Long)

    /** Force-clear every toast currently in the stack. */
    public fun clear()
}

/** CompositionLocal for providing [ToastController] throughout the Compose tree. */
public val LocalToastController: CompositionLocal<ToastController?> =
    staticCompositionLocalOf {
        null
    }
