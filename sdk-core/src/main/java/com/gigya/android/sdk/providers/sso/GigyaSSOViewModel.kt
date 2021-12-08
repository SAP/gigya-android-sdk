package com.gigya.android.sdk.providers.sso

import android.content.Context
import com.gigya.android.sdk.Config
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.api.GigyaApiRequest
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.network.adapter.IRestAdapter
import com.gigya.android.sdk.network.adapter.IRestAdapterCallback
import com.gigya.android.sdk.network.adapter.RestAdapter
import com.gigya.android.sdk.session.SessionInfo
import com.gigya.android.sdk.utils.PKCEHelper
import java.net.URLEncoder
import com.google.gson.Gson
import java.util.*


class GigyaSSOViewModel(val context: Context, val config: Config, val restAdapter: IRestAdapter) {

    var pkceHelper: PKCEHelper = PKCEHelper()
    val gson: Gson = Gson()

    private val fidmPath = "/oidc/op/v1.0/"
    private val fidmUrl = "https://fidm."

    init {
        // Create new PKCE challenge/verifier.
        pkceHelper.newChallenge()
    }

    /**
     * Build fidm base url.
     */
    fun getUrl(path: String): String =
            "$fidmUrl${(config.apiDomain ?: "")}$fidmPath${config.apiKey ?: ""}/$path"

    /**
     * Get authorization URL.
     * URL will be used in the Custom tab implementation to authenticate the user.
     */
    fun getAuthorizeUrl(): String {
        val urlString = getUrl("authorize")
        val serverParams: Map<String, String> = mapOf(
                "redirect_uri" to "gsapi://login/",
                "response_type" to "code",
                "client_id" to config.apiKey,
                "scope" to "gigya.sso",
                "code_challenge" to pkceHelper.challenge!!,
                "code_challenge_method" to "S256"
        )
        val queryString = serverParams.entries.joinToString("&")
        { "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}" }
        return "$urlString?$queryString"
    }

    /**
     * Exchange authentication code for access token.
     */
    fun getToken(code: String, onComplete: () -> Unit, onError: (GigyaError) -> Unit) {
        GigyaLogger.debug("OIDCWrapper", "getToken called with auth code ->$code")

        val serverParams = TreeMap<String, Any>()
        serverParams["redirect_uri"] = "gsapi://login/"
        serverParams["client_id"] = config.apiKey
        serverParams["grant_type"] = "authorization_code"
        serverParams["code"] = code
        serverParams["code_verifier"] = pkceHelper.verifier!!
        val urlString = getUrl("token")
        val request = GigyaApiRequest(RestAdapter.HttpMethod.POST, urlString, serverParams)

        restAdapter.sendUnsigned(request, object : IRestAdapterCallback() {
            override fun onResponse(jsonResponse: String, responseDateHeader: String) {
                GigyaLogger.debug("OIDCWrapper", "getToken: success -> $jsonResponse")
                val map: Map<String, String> = gson.fromJson(jsonResponse, Map::class.java) as Map<String, String>
                if (map.containsKey("access_token")) {
                    val token = map["access_token"] as String
                    // Exchange token for JWT containing the required session info object.
                    getSession(token, onComplete, onError)
                } else {
                    //TODO ERROR
                }
            }

            override fun onError(gigyaError: GigyaError) {
                GigyaLogger.debug("OIDCWrapper", "getToken: fail -> $gigyaError")
                //TODO ERROR
            }
        })
    }

    /**
     * Exchange access token for a JWT token that contains the user session.
     */
    private fun getSession(token: String, onComplete: () -> Unit, onError: (GigyaError) -> Unit) {
        GigyaLogger.debug("OIDCWrapper", "getJWT called with token ->$token")

        val serverParams = TreeMap<String, Any>()
        serverParams["redirect_uri"] = "gsapi://login/"
        serverParams["client_id"] = config.apiKey
        serverParams["scope"] = "gigya.mobileSessionInfo"
        serverParams["grant_type"] = "urn:ietf:params:oauth:grant-type:token-exchange"
        serverParams["requested_token_type"] = "urn:ietf:params:oauth:token-type:jwt"
        serverParams["subject_token_type"] = "urn:ietf:params:oauth:token-type:access_token"
        serverParams["subject_token"] = token
        val url = getUrl("session")
        val request = GigyaApiRequest(RestAdapter.HttpMethod.POST, url, serverParams)

        restAdapter.sendUnsigned(request, object : IRestAdapterCallback() {
            override fun onResponse(jsonResponse: String, responseDateHeader: String) {
                GigyaLogger.debug("OIDCWrapper", "getJWT: success -> $jsonResponse")
            }

            override fun onError(gigyaError: GigyaError) {
                GigyaLogger.debug("OIDCWrapper", "getJWT: fail -> $gigyaError")
                //TODO ERROR
            }
        })
    }

    /**
     * Parse JWT to retrieve the required session info object.
     */
    private fun parseJWT(jwt: String): SessionInfo? {
        return null
    }

    /**
     * Set the session and retrieve account information.
     */
    private fun setSession(info: SessionInfo) {

    }
}
