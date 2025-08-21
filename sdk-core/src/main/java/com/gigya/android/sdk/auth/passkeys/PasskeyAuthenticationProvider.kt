package com.gigya.android.sdk.auth.passkeys

import android.annotation.SuppressLint
import android.content.Context
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import com.gigya.android.sdk.GigyaLogger
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class PasskeysAuthenticationProvider(
    private val weakActivity: WeakReference<Context>? = null
) : IPasskeysAuthenticationProvider {

    companion object {
        const val LOG_TAG = "PasskeysAuthenticationProvider"
    }

    private val credentialManager by lazy(LazyThreadSafetyMode.PUBLICATION) {
        weakActivity?.get()?.let {
            CredentialManager.create(it)
        }
    }

    private val executor: Executor = Executors.newSingleThreadExecutor()

    @SuppressLint("PublicKeyCredential")
    override fun createPasskey(requestJson: String): CompletableFuture<String?> {
        val future = CompletableFuture<String?>()
        val context = weakActivity?.get()
        if (context == null) {
            future.complete(null)
            return future
        }
        try {
            val createPublicKeyCredentialRequest = CreatePublicKeyCredentialRequest(
                // Contains the request in JSON format. Uses the standard WebAuthn
                // web JSON spec.
                requestJson = requestJson,
                // Defines whether you prefer to use only immediately available credentials,
                // not hybrid credentials, to fulfill this request. This value is false
                // by default.
                preferImmediatelyAvailableCredentials = false,
            )
            credentialManager?.createCredentialAsync(
                context,
                createPublicKeyCredentialRequest,
                null, // Optional CancellationSignal
                executor,
                object :
                    CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException> {
                    override fun onResult(result: CreateCredentialResponse) {
                        val resultJson = (result as? CreatePublicKeyCredentialResponse)?.registrationResponseJson
                        future.complete(resultJson)
                    }

                    override fun onError(e: CreateCredentialException) {
                        GigyaLogger.debug(LOG_TAG, e.message ?: "Error creating passkey")
                        future.complete(null)
                    }
                }
            )
        } catch (e: Exception) {
            GigyaLogger.debug(LOG_TAG, e.message ?: "Error creating passkey")
            future.complete(null)
        }
        return future
    }

    override fun getPasskey(requestJson: String): CompletableFuture<String?> {
        val future = CompletableFuture<String?>()
        val context = weakActivity?.get()
        if (context == null) {
            future.complete(null)
            return future
        }
        try {
            // Get passkeys from the user's public key credential provider.
            val getPublicKeyCredentialOption = GetPublicKeyCredentialOption(
                requestJson = requestJson,
            )

            val getCredRequest = GetCredentialRequest(
                listOf(getPublicKeyCredentialOption)
            )
            credentialManager?.getCredentialAsync(
                context,
                getCredRequest,
                null, // Optional CancellationSignal
                executor,
                object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                    override fun onResult(result: GetCredentialResponse) {
                        future.complete(result.credential.data.getString("androidx.credentials.BUNDLE_KEY_AUTHENTICATION_RESPONSE_JSON"))
                    }

                    override fun onError(e: GetCredentialException) {
                        GigyaLogger.debug(LOG_TAG, e.message ?: "Error getting passkey")
                        future.complete(null)
                    }
                }
            )
        } catch (e: Exception) {
            GigyaLogger.debug(LOG_TAG, e.message ?: "Error getting passkey")
            future.complete(null)
        }
        return future
    }
}