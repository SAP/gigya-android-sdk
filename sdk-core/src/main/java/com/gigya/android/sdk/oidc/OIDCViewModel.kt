package com.gigya.android.sdk.oidc

import android.content.Context
import com.gigya.android.sdk.Config
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.api.GigyaApiRequest
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.network.adapter.IRestAdapter
import com.gigya.android.sdk.network.adapter.IRestAdapterCallback
import com.gigya.android.sdk.network.adapter.RestAdapter
import com.gigya.android.sdk.utils.PKCEHelper
import java.net.URLEncoder
import com.google.gson.Gson
import java.util.*


class OIDCViewModel(val context: Context, val config: Config, val restAdapter: IRestAdapter) {

    var pkceHelper: PKCEHelper = PKCEHelper()

    private val fidmPath = "/oidc/op/v1.0/"
    private val fidmUrl = "https://fidm."

    init {
        pkceHelper.newChallenge()
    }

    fun getUrl(path: String): String =
            "$fidmUrl${(config.apiDomain ?: "")}$fidmPath${config.apiKey ?: ""}/$path"


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

    fun authenticateWith(code: String, completionHandler: () -> Unit) {
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
                GigyaLogger.debug("OIDCWrapper", "sendErrorReport: success")
                val gson = Gson()
                val map: Map<String, String> = gson.fromJson(jsonResponse, Map::class.java) as Map<String, String>
                if (map.containsKey("access_token")) {
                    val token = map["access_token"] as String
                    getSession(token, completionHandler)
                } else {
                    //TODO ERROR
                }
            }

            override fun onError(gigyaError: GigyaError) {
                GigyaLogger.debug("OIDCWrapper", "sendErrorReport: fail")
                //TODO ERROR
            }
        })
    }

    private fun getSession(token: String, completionHandler: () -> Unit) {
        val serverParams = TreeMap<String, Any>()
        serverParams["redirect_uri"] = "gsapi://login/"
        serverParams["client_id"] = config.apiKey
        serverParams["grant_type"] = "token-exchange"
        serverParams["requested_token_type"] = "jwt"
        serverParams["subject_token_type"] = "access_token"
        serverParams["subject_toke"] = token
        val url = getUrl("session")
        val request = GigyaApiRequest(RestAdapter.HttpMethod.POST, url, serverParams)
        restAdapter.sendUnsigned(request, object : IRestAdapterCallback() {
            override fun onResponse(jsonResponse: String, responseDateHeader: String) {
                GigyaLogger.debug("OIDCWrapper", "sendErrorReport: success")
            }

            override fun onError(gigyaError: GigyaError) {
                GigyaLogger.debug("OIDCWrapper", "sendErrorReport: fail")
                //TODO ERROR
            }
        })
    }
}
