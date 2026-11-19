package app.simt.ui.features.auth

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import app.simt.ui.resources.Res
import app.simt.ui.resources.simt_ui_config_error_client_id
import app.simt.ui.resources.simt_ui_google_sign_in_failed
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import java.util.Locale

/** Value the library `default_web_client_id` resource holds until google-services overrides it. */
private const val PLACEHOLDER_CLIENT_ID = "placeholder"

/**
 * Stateful controller for the Google sign-in flow built on Credential Manager.
 *
 * Create one with [rememberGoogleSignInLauncher] and call [launch] from a click handler. The whole
 * Credential Manager dance (saved-account request, web sign-in fallback, ID-token extraction and
 * error handling) lives here so feature screens only supply what to do with the resulting token.
 */
@Stable
public class GoogleSignInLauncher internal constructor(
    private val scope: CoroutineScope,
    private val context: Context,
    private val credentialManager: CredentialManager,
    private val webClientId: String,
    private val configErrorMessage: String,
    private val genericErrorMessage: String,
    private val onIdToken: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onStart: () -> Unit,
) {
    /** True while a sign-in attempt is in flight; drives button/progress UI. */
    public var isSigningIn: Boolean by mutableStateOf(false)
        private set

    /**
     * Starts the Google sign-in flow. No-op if an attempt is already running.
     *
     * On success the obtained ID token is delivered to `onIdToken`; any failure (including a
     * misconfigured `default_web_client_id`) is reported through `onError` with a ready-to-display
     * message.
     */
    public fun launch() {
        if (isSigningIn) return
        onStart()
        if (webClientId.isBlank() || webClientId == PLACEHOLDER_CLIENT_ID) {
            onError(configErrorMessage)
            return
        }
        isSigningIn = true
        scope.launch {
            try {
                onIdToken(credentialManager.requestGoogleIdToken(context, webClientId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError(e.message ?: genericErrorMessage)
            } finally {
                isSigningIn = false
            }
        }
    }
}

/**
 * Remembers a [GoogleSignInLauncher] for the given [webClientId].
 *
 * The application resolves the client id itself - typically the `default_web_client_id` string
 * that the google-services plugin injects at build time - and passes it in, so no host resources
 * are read here.
 *
 * @param webClientId The OAuth web client id for the Google sign-in flow.
 * @param onIdToken Called with the Google ID token once sign-in succeeds.
 * @param onError Called with a user-facing message when sign-in fails or is misconfigured.
 * @param onStart Called the moment a sign-in attempt begins (e.g. to log analytics). Defaults to no-op.
 */
@Composable
public fun rememberGoogleSignInLauncher(
    webClientId: String,
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit,
    onStart: () -> Unit = {},
): GoogleSignInLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val configErrorMessage = stringResource(Res.string.simt_ui_config_error_client_id)
    val genericErrorMessage = stringResource(Res.string.simt_ui_google_sign_in_failed)

    val latestOnIdToken = rememberUpdatedState(onIdToken)
    val latestOnError = rememberUpdatedState(onError)
    val latestOnStart = rememberUpdatedState(onStart)

    return remember(scope, context, credentialManager, webClientId, configErrorMessage, genericErrorMessage) {
        GoogleSignInLauncher(
            scope = scope,
            context = context,
            credentialManager = credentialManager,
            webClientId = webClientId,
            configErrorMessage = configErrorMessage,
            genericErrorMessage = genericErrorMessage,
            onIdToken = { latestOnIdToken.value(it) },
            onError = { latestOnError.value(it) },
            onStart = { latestOnStart.value() },
        )
    }
}

/**
 * Runs the Credential Manager flow and returns the Google ID token.
 *
 * Tries the saved-account flow first ([GetGoogleIdOption]); if no device account is available it
 * falls back to Google's full web sign-in ([GetSignInWithGoogleOption]).
 */
private suspend fun CredentialManager.requestGoogleIdToken(
    context: Context,
    webClientId: String,
): String {
    val credential =
        try {
            getCredential(context, googleIdRequest(webClientId)).credential
        } catch (e: GetCredentialException) {
            if (e !is NoCredentialException && !isNoCredentialsAvailableError(e.message)) throw e
            getCredential(context, signInWithGoogleRequest(webClientId)).credential
        }
    return credential.googleIdToken()
}

private fun googleIdRequest(webClientId: String): GetCredentialRequest =
    GetCredentialRequest.Builder()
        .addCredentialOption(
            GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build(),
        )
        .build()

private fun signInWithGoogleRequest(webClientId: String): GetCredentialRequest =
    GetCredentialRequest.Builder()
        .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
        .build()

private fun Credential.googleIdToken(): String =
    when {
        this is GoogleIdTokenCredential -> idToken
        type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ->
            GoogleIdTokenCredential.createFrom(data).idToken
        else -> error("Unexpected credential type: $type")
    }

private fun isNoCredentialsAvailableError(message: String?): Boolean = message?.lowercase(Locale.ROOT)?.contains("no credentials available") == true
