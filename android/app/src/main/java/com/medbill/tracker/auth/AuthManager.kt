package com.medbill.tracker.auth

import android.app.Activity
import android.content.Context
import com.medbill.tracker.BuildConfig
import com.medbill.tracker.R
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SignInParameters
import com.microsoft.identity.client.SilentAuthenticationCallback
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class AuthManager(context: Context) {
    private val appContext = context.applicationContext
    private val tenantAuthority = "https://login.microsoftonline.com/${BuildConfig.ENTRA_TENANT}"

    private suspend fun getClient(): ISingleAccountPublicClientApplication {
        return suspendCancellableCoroutine { continuation ->
            PublicClientApplication.createSingleAccountPublicClientApplication(
                appContext,
                R.raw.auth_config_single_account,
                object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                    override fun onCreated(application: ISingleAccountPublicClientApplication) {
                        continuation.resume(application)
                    }

                    override fun onError(exception: com.microsoft.identity.client.exception.MsalException) {
                        continuation.resumeWithException(exception)
                    }
                }
            )
        }
    }

    suspend fun signIn(activity: Activity): IAuthenticationResult {
        val client = getClient()
        return suspendCancellableCoroutine { continuation ->
            val params = SignInParameters.builder()
                .withActivity(activity)
                .withScopes(listOf(BuildConfig.ENTRA_API_SCOPE))
                .withCallback(object : com.microsoft.identity.client.AuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult) {
                        continuation.resume(authenticationResult)
                    }

                    override fun onError(exception: com.microsoft.identity.client.exception.MsalException) {
                        continuation.resumeWithException(exception)
                    }

                    override fun onCancel() {
                        continuation.resumeWithException(IllegalStateException("Sign-in canceled"))
                    }
                })
                .build()

            client.signIn(params)
        }
    }

    suspend fun restoreExistingSession(): IAuthenticationResult? {
        val client = getClient()
        val account = getCurrentAccount(client) ?: return null
        return acquireTokenSilent(client, account)
    }

    private suspend fun getCurrentAccount(client: ISingleAccountPublicClientApplication): IAccount? {
        return suspendCancellableCoroutine { continuation ->
            client.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
                override fun onAccountLoaded(activeAccount: IAccount?) {
                    continuation.resume(activeAccount)
                }

                override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                }

                override fun onError(exception: com.microsoft.identity.client.exception.MsalException) {
                    continuation.resumeWithException(exception)
                }
            })
        }
    }

    private suspend fun acquireTokenSilent(
        client: ISingleAccountPublicClientApplication,
        account: IAccount
    ): IAuthenticationResult {
        return suspendCancellableCoroutine { continuation ->
            val params = AcquireTokenSilentParameters.Builder()
                .forAccount(account)
                .withScopes(listOf(BuildConfig.ENTRA_API_SCOPE))
                .fromAuthority(tenantAuthority)
                .withCallback(object : SilentAuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult) {
                        continuation.resume(authenticationResult)
                    }

                    override fun onError(exception: com.microsoft.identity.client.exception.MsalException) {
                        continuation.resumeWithException(exception)
                    }
                })
                .build()

            client.acquireTokenSilentAsync(params)
        }
    }

    suspend fun signOut() {
        val client = getClient()
        suspendCancellableCoroutine { continuation ->
            client.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
                override fun onSignOut() {
                    continuation.resume(Unit)
                }

                override fun onError(exception: com.microsoft.identity.client.exception.MsalException) {
                    continuation.resumeWithException(exception)
                }
            })
        }
    }
}
