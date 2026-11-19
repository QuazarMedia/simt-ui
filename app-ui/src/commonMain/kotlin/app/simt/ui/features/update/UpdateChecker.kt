package app.simt.ui.features.update

/**
 * Outcome of an update availability check.
 */
public sealed interface UpdateCheckResult {
    /** The store offers a newer version for this installation. */
    public data object Available : UpdateCheckResult

    /** No update is available, or the check could not establish one. */
    public data object Unavailable : UpdateCheckResult
}

/**
 * Checks whether an update for the host application is available and starts
 * the platform update flow when the user confirms.
 *
 * Implementations own the platform update machinery and must be safe to call
 * from any coroutine: they do not throw, and they degrade to
 * [UpdateCheckResult.Unavailable] when the platform update service is not
 * usable (sideloaded installation, store outage). [UpdateAvailableDialog] is
 * the UI counterpart of this contract: show it when [checkForUpdate] returns
 * [UpdateCheckResult.Available] and call [startUpdateFlow] from its update
 * action.
 */
public interface UpdateChecker {
    /**
     * Checks whether an update is available.
     *
     * @return [UpdateCheckResult.Available] when a newer version can be
     *   installed, [UpdateCheckResult.Unavailable] otherwise.
     */
    public suspend fun checkForUpdate(): UpdateCheckResult

    /**
     * Starts the platform update flow (for example the Play in-app update
     * UI).
     *
     * @param activityProvider Supplies the host activity; the flow is a no-op
     *   when it returns null.
     */
    public fun startUpdateFlow(activityProvider: () -> Any?)
}
