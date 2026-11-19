package app.simt.ui.components.loader

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.StateFlow

@Immutable
public data class BlockingLoaderUiState(
    public val visible: Boolean,
    public val message: String? = null,
    public val progress: Float? = null, // 0.0 to 1.0, or null for indeterminate
    public val onInterrupt: (suspend () -> Boolean)? = null, // Interrupt callback that returns true if successful, false otherwise
    public val showBackgroundProcessingWarning: Boolean = false,
    public val backgroundProcessingWarningMessage: String? = null,
)

/**
 * App-wide controller for a single blocking loader overlay.
 *
 * Designed to be injected into any module/viewmodel.
 * Supports multiple concurrent requests via tokens (stack/reference-count style).
 */
public interface BlockingLoaderController {
    public val uiState: StateFlow<BlockingLoaderUiState>

    /** Show the global loader. Returns a token that must be used to hide/update. */
    public fun show(
        message: String? = null,
        progress: Float? = null,
        onInterrupt: (suspend () -> Boolean)? = null,
        showBackgroundProcessingWarning: Boolean = false,
        backgroundProcessingWarningMessage: String? = null,
    ): String

    /** Update the message and/or progress for an existing token. */
    public fun update(
        token: String,
        message: String? = null,
        progress: Float? = null,
        showBackgroundProcessingWarning: Boolean? = null,
        backgroundProcessingWarningMessage: String? = null,
    )

    /** Hide a previously shown loader request identified by [token]. */
    public fun hide(token: String)

    /** Force-clear all outstanding loader requests. Use sparingly. */
    public fun clear()

    /** Request interrupt of the current loader. Returns true if interrupt was successful, false otherwise. */
    public suspend fun requestInterrupt(): Boolean
}

/**
 * CompositionLocal for providing [BlockingLoaderController] throughout the Compose tree.
 */
public val LocalBlockingLoaderState: ProvidableCompositionLocal<BlockingLoaderController?> =
    staticCompositionLocalOf<BlockingLoaderController?> {
        null
    }
