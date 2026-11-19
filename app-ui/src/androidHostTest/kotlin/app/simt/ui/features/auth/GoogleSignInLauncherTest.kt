package app.simt.ui.features.auth

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.CancellationSignal
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CreateCredentialRequest
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PrepareGetCredentialResponse
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.ClearCredentialUnknownException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.Executor

/**
 * Coverage for the Google sign-in launcher: the misconfigured-client-id gate (synchronous,
 * deterministic), the sign-in attempt's error path and the [GoogleSignInLauncher.isSigningIn]
 * reset, and the in-flight no-op guard.
 *
 * The sign-in attempt is driven with a [FailingCredentialManager] because the real Credential
 * Manager opens a Play Services connection that never completes under the host; the double fails
 * immediately so the error path and state reset are what get exercised.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@OptIn(ExperimentalTestApi::class)
class GoogleSignInLauncherTest {
    @get:Rule
    val compose = createComposeRule()

    private fun launchIn(
        webClientId: String,
        onIdToken: (String) -> Unit,
        onError: (String) -> Unit,
    ): GoogleSignInLauncher {
        var launcher: GoogleSignInLauncher? = null
        compose.setContent {
            MaterialTheme {
                launcher =
                    rememberGoogleSignInLauncher(
                        webClientId = webClientId,
                        onIdToken = onIdToken,
                        onError = onError,
                    )
            }
        }
        return launcher!!
    }

    @Test
    fun `placeholder client id reports the configuration error synchronously`() {
        var token: String? = null
        var error: String? = null
        val launcher = launchIn("placeholder", onIdToken = { token = it }, onError = { error = it })

        launcher.launch()

        assertNull("no token must be delivered for a placeholder client id", token)
        assertNotNull("a configuration error must be reported", error)
        assertFalse("isSigningIn must be false after a synchronous rejection", launcher.isSigningIn)
    }

    @Test
    fun `blank client id reports the configuration error synchronously`() {
        var token: String? = null
        var error: String? = null
        val launcher = launchIn("", onIdToken = { token = it }, onError = { error = it })

        launcher.launch()

        assertNull(token)
        assertNotNull(error)
        assertFalse(launcher.isSigningIn)
    }

    @Test
    fun `sign-in attempt that finds no credentials reports an error and resets state`() {
        val scope = TestScope(StandardTestDispatcher())
        var token: String? = null
        var error: String? = null
        val launcher =
            GoogleSignInLauncher(
                scope = scope,
                context = ApplicationProvider.getApplicationContext(),
                credentialManager = FailingCredentialManager,
                webClientId = "123456789.apps.googleusercontent.com",
                configErrorMessage = "config error",
                genericErrorMessage = "generic error",
                onIdToken = { token = it },
                onError = { error = it },
                onStart = {},
            )

        launcher.launch()
        scope.advanceUntilIdle()

        assertNull("no token can be obtained when no credential is available", token)
        assertNotNull("the failed attempt must report an error message", error)
        assertFalse("isSigningIn must be reset after the attempt finishes", launcher.isSigningIn)
    }

    @Test
    fun `launch is a no-op while an attempt is already in flight`() {
        var starts = 0
        var launcher: GoogleSignInLauncher? = null
        compose.setContent {
            MaterialTheme {
                launcher =
                    rememberGoogleSignInLauncher(
                        webClientId = "123456789.apps.googleusercontent.com",
                        onIdToken = {},
                        onError = {},
                        onStart = { starts++ },
                    )
            }
        }
        val active = launcher!!

        active.launch()
        active.launch()
        compose.waitForIdle()

        assertEquals("onStart must fire exactly once for overlapping launches", 1, starts)
    }

    /**
     * A [CredentialManager] whose requests fail immediately with [NoCredentialException], standing
     * in for the Play Services connection that is unavailable under the host.
     */
    private object FailingCredentialManager : CredentialManager {
        override fun getCredentialAsync(
            context: Context,
            request: GetCredentialRequest,
            cancellationSignal: CancellationSignal?,
            executor: Executor,
            callback: CredentialManagerCallback<GetCredentialResponse, GetCredentialException>,
        ) {
            callback.onError(NoCredentialException("no credentials available"))
        }

        override fun getCredentialAsync(
            context: Context,
            request: PrepareGetCredentialResponse.PendingGetCredentialHandle,
            cancellationSignal: CancellationSignal?,
            executor: Executor,
            callback: CredentialManagerCallback<GetCredentialResponse, GetCredentialException>,
        ) {
            callback.onError(NoCredentialException("no credentials available"))
        }

        override fun prepareGetCredentialAsync(
            request: GetCredentialRequest,
            cancellationSignal: CancellationSignal?,
            executor: Executor,
            callback: CredentialManagerCallback<PrepareGetCredentialResponse, GetCredentialException>,
        ) {
            callback.onError(NoCredentialException("no credentials available"))
        }

        override fun createCredentialAsync(
            context: Context,
            request: CreateCredentialRequest,
            cancellationSignal: CancellationSignal?,
            executor: Executor,
            callback: CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException>,
        ) {
            callback.onError(CreateCredentialUnknownException("no credentials available"))
        }

        override fun clearCredentialStateAsync(
            request: ClearCredentialStateRequest,
            cancellationSignal: CancellationSignal?,
            executor: Executor,
            callback: CredentialManagerCallback<Void?, ClearCredentialException>,
        ) {
            callback.onError(ClearCredentialUnknownException("no credentials available"))
        }

        override fun createSettingsPendingIntent(): PendingIntent {
            val context = ApplicationProvider.getApplicationContext<Context>()
            return PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
        }
    }
}
