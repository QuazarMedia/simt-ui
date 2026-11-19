package app.simt.ui.components.loader

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.Closeable

// Show the generic "keep the app foregrounded" warning for medium-length waits.
public const val BLOCKING_LOADER_WARNING_THRESHOLD_MS: Long = 10000L

// Switch to the GNSS-specific background-processing guidance once waits become clearly long-running.
public const val BACKGROUND_PROCESSING_WARNING_THRESHOLD_MS: Long = 20000L

/**
 * Handle returned by [BlockingLoaderController.showHandle]. Always call [close] to hide.
 *
 * Intended usage:
 * `blockingLoaderController.showHandle("Saving...").use { ... }`
 */
public class BlockingLoaderHandle internal constructor(
    private val controller: BlockingLoaderController,
    private val token: String,
) : Closeable {
    public fun update(
        message: String? = null,
        progress: Float? = null,
        showBackgroundProcessingWarning: Boolean? = null,
        backgroundProcessingWarningMessage: String? = null,
    ) {
        controller.update(
            token,
            message,
            progress,
            showBackgroundProcessingWarning,
            backgroundProcessingWarningMessage,
        )
    }

    public override fun close() {
        controller.hide(token)
    }
}

/**
 * Convenience to ensure the loader is always hidden.
 */
public fun BlockingLoaderController.showHandle(
    message: String? = null,
    progress: Float? = null,
    onInterrupt: (suspend () -> Boolean)? = null,
    showBackgroundProcessingWarning: Boolean = false,
    backgroundProcessingWarningMessage: String? = null,
): BlockingLoaderHandle {
    return BlockingLoaderHandle(
        this,
        show(
            message,
            progress,
            onInterrupt,
            showBackgroundProcessingWarning,
            backgroundProcessingWarningMessage,
        ),
    )
}

/**
 * Suspend variant of [withBlockingLoader].
 */
public suspend inline fun <T> BlockingLoaderController.withBlockingLoader(
    message: String? = null,
    progress: Float? = null,
    showBackgroundProcessingWarning: Boolean = false,
    backgroundProcessingWarningMessage: String? = null,
    crossinline block: suspend (handle: BlockingLoaderHandle) -> T,
): T {
    val handle =
        showHandle(
            message,
            progress,
            null,
            showBackgroundProcessingWarning,
            backgroundProcessingWarningMessage,
        )
    return try {
        block(handle)
    } catch (e: CancellationException) {
        throw e
    } finally {
        handle.close()
    }
}

/**
 * Suspend variant of [withBlockingLoader] with interrupt support.
 *
 * @param onInterrupt Callback that attempts to interrupt the process.
 *                    Should return true if interruption was successful, false otherwise.
 *                    If true is returned, the loader will be hidden immediately.
 *                    If false is returned, the loader will wait for the process to complete.
 */
public suspend inline fun <T> BlockingLoaderController.withBlockingLoader(
    message: String? = null,
    progress: Float? = null,
    showBackgroundProcessingWarning: Boolean = false,
    backgroundProcessingWarningMessage: String? = null,
    noinline onInterrupt: (suspend () -> Boolean)?,
    crossinline block: suspend (handle: BlockingLoaderHandle) -> T,
): T {
    val handle =
        showHandle(
            message,
            progress,
            onInterrupt,
            showBackgroundProcessingWarning,
            backgroundProcessingWarningMessage,
        )
    return try {
        block(handle)
    } catch (e: CancellationException) {
        throw e
    } finally {
        handle.close()
    }
}

/**
 * Specialized helper for operations that show a progress bar over time (e.g. GNSS location fetch).
 *
 * @param message The message to show.
 * @param timeoutMs The total duration for the progress bar (100%).
 * @param showProgress Whether to actually show the progress bar.
 * @param onInterrupt Optional interrupt callback.
 * @param checkAbort Optional lambda to check if we should stop the progress (e.g. permission lost).
 *                   Should return true if the operation should be aborted.
 */
public suspend inline fun <T> BlockingLoaderController.withTimedProgress(
    message: String? = null,
    timeoutMs: Long,
    showProgress: Boolean,
    showBackgroundProcessingWarning: Boolean = false,
    backgroundProcessingWarningMessage: String? = null,
    noinline onInterrupt: (suspend () -> Boolean)? = null,
    noinline checkAbort: (() -> Boolean)? = null,
    crossinline block: suspend (handle: BlockingLoaderHandle) -> T,
): T {
    // The generic warning appears after 10s unless a caller explicitly forces it sooner.
    val shouldShowWarning = showBackgroundProcessingWarning || timeoutMs > BLOCKING_LOADER_WARNING_THRESHOLD_MS

    return withBlockingLoader(
        message = message,
        onInterrupt = onInterrupt,
        showBackgroundProcessingWarning = shouldShowWarning,
        backgroundProcessingWarningMessage = backgroundProcessingWarningMessage,
    ) { handle ->
        coroutineScope {
            var progressJob: Job? = null
            if (showProgress && timeoutMs > 0) {
                progressJob =
                    launch {
                        val startTime = System.currentTimeMillis()
                        handle.update(progress = 0f)

                        while (isActive && System.currentTimeMillis() - startTime < timeoutMs) {
                            if (checkAbort?.invoke() == true) {
                                return@launch
                            }
                            val elapsed = System.currentTimeMillis() - startTime
                            val progress = (elapsed.toFloat() / timeoutMs).coerceIn(0f, 1f)

                            handle.update(
                                progress = progress,
                                showBackgroundProcessingWarning = shouldShowWarning,
                                backgroundProcessingWarningMessage = backgroundProcessingWarningMessage,
                            )
                            delay(500)
                        }
                        handle.update(progress = 1f)
                    }
            }

            try {
                block(handle)
            } finally {
                progressJob?.cancel()
            }
        }
    }
}
