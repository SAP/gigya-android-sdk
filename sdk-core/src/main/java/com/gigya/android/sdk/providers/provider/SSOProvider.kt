package com.gigya.android.sdk.providers.provider

import android.app.Activity
import android.content.Context
import android.net.Uri
import com.gigya.android.sdk.Config
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.api.GigyaApiRequest
import com.gigya.android.sdk.api.GigyaApiResponse
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.network.adapter.IRestAdapter
import com.gigya.android.sdk.network.adapter.IRestAdapterCallback
import com.gigya.android.sdk.network.adapter.RestAdapter
import com.gigya.android.sdk.persistence.IPersistenceService
import com.gigya.android.sdk.providers.sso.GigyaSSOLoginActivity
import com.gigya.android.sdk.providers.sso.GigyaSSOLoginActivity.SSOLoginActivityCallback
import com.gigya.android.sdk.session.SessionInfo
import com.gigya.android.sdk.utils.PKCEHelper
import com.gigya.android.sdk.utils.UrlUtils
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder
import java.util.*

class SSOProvider(
    var context: Context?,
    persistenceService: IPersistenceService?,
    providerCallback: ProviderCallback?,
    val restAdapter: IRestAdapter?,
    var config: Config?
) : Provider(context, persistenceService, providerCallback) {

    companion object {

        private const val LOG_TAG = "SSOProvider"
        private const val fidmPath = "/oidc/op/v1.0/"
        private const val fidmUrl = "https://fidm."

        // Paths
        private const val AUTHORIZE = "authorize"
        private const val TOKEN = "token"
    }

    override fun getName(): String = "sso"

    private var pkceHelper: PKCEHelper = PKCEHelper()
    val gson: Gson = Gson()

    var packageName: String

    var redirect: String? = null

    init {
        // Create new PKCE challenge/verifier.
        pkceHelper.newChallenge()
        packageName = _context.packageName
    }

    private fun getContext(params: MutableMap<String, Any>?): String? {
        if (params == null) return null
        if (params.isEmpty()) return null
        return Gson().toJson(params)
    }

    /**
     * Initiate provider login.
     */
    override fun login(params: MutableMap<String, Any>?, loginMode: String?) {
        if (_connecting) {
            return
        }
        _connecting = true

        // Irrelevant...
        _loginMode = loginMode

        val loginUrl: String = getAuthorizeUrl(params)
        GigyaLogger.debug(LOG_TAG, "login: with url $loginUrl")

        // Authorize endpoint.
        GigyaSSOLoginActivity.present(context!!, loginUrl, object : SSOLoginActivityCallback {
            override fun onResult(activity: Activity?, uri: Uri) {
                GigyaLogger.debug(LOG_TAG, "GigyaSSOLoginActivity: onResult -> ${uri.query}")

                // Parse Uri query parameters.
                val parsed = getQueryKeyValueMap(uri)

                if (parsed.containsKey("code")) {
                    onSSOCodeReceived(parsed["code"] as String)
                } else {
                    if (parsed.containsKey("error")) {
                        val jsonError = parseErrorUri(parsed["error_uri"] as String)
                        onLoginFailed(GigyaApiResponse(jsonError))
                    } else {
                        // Failed login authorization.
                        onLoginFailed(GigyaApiResponse(GigyaError.generalError().toString()))
                    }
                }
            }

            override fun onCancelled() {
                _connecting = false
                GigyaLogger.debug(LOG_TAG, "GigyaSSOLoginActivity: onCancelled")
                _providerCallback?.onCanceled()
            }

        })
    }

    // Not used in this provider instance.
    override fun getProviderSessions(tokenOrCode: String?, expiration: Long, uid: String?): String {
        return ""
    }

    /**
     * Build fidm base url.
     */
    fun getUrl(path: String): String =
        when (config!!.isCnameEnabled) {
            true -> "https://${config!!.cname}$fidmPath${config!!.apiKey ?: ""}/$path"
            false -> "$fidmUrl${config!!.apiDomain}$fidmPath${config!!.apiKey ?: ""}/$path"
        }


    /**
     * Get authorization URL.
     * URL will be used in the Custom tab implementation to authenticate the user.
     */
    private fun getAuthorizeUrl(params: MutableMap<String, Any>?): String {
        val urlString = getUrl(AUTHORIZE)

        var redirectUri = "gsapi://${packageName}/login/"
        if (redirect != null) {
            redirectUri = redirect!!
        }

        val serverParams: MutableMap<String, Any> = mutableMapOf(
            "redirect_uri" to redirectUri,
            "response_type" to "code",
            "client_id" to config!!.apiKey,
            "scope" to "device_sso",
            "code_challenge" to pkceHelper.challenge!!,
            "code_challenge_method" to "S256"
        )

        // Evaluate context & parameters.
        params?.let {
            params.forEach { entry ->
                print("${entry.key} : ${entry.value}")
                if (entry.value is Map<*, *>) {
                    val json = Gson().toJson(entry.value)
                    params[entry.key] = json
                }
            }
            serverParams += params
        }

        val queryString = serverParams.entries.joinToString("&")
        { "${it.key}=${URLEncoder.encode(it.value.toString(), "UTF-8")}" }
        return "$urlString?$queryString"
    }

    /**
     * SSO code received and will be exchanged for a valid session.
     */
    private fun onSSOCodeReceived(code: String) {
        GigyaLogger.debug(LOG_TAG, "onSSOCodeReceived: with code $code")

        var redirectUri = "gsapi://${packageName}/login/"
        if (redirect != null) {
            redirectUri = redirect!!
        }

        val headers = hashMapOf(
            "apikey" to config!!.apiKey
        )

        val serverParams = TreeMap<String, Any>()
        serverParams["redirect_uri"] = redirectUri
        serverParams["client_id"] = config!!.apiKey
        serverParams["grant_type"] = "authorization_code"
        serverParams["code"] = code
        serverParams["code_verifier"] = pkceHelper.verifier!!
        val urlString = getUrl(TOKEN)
        val request = GigyaApiRequest(RestAdapter.HttpMethod.POST, urlString, serverParams, headers)

        // Token endpoint.
        restAdapter?.sendUnsigned(request, object : IRestAdapterCallback() {
            override fun onResponse(jsonResponse: String, responseDateHeader: String) {
                GigyaLogger.debug(LOG_TAG, "getToken: success -> $jsonResponse")

                // Parse response.
                val parsed: Map<String, Any> =
                    gson.fromJson(jsonResponse, Map::class.java) as Map<String, Any>
                when {
                    parsed.containsKey("access_token") -> {
                        val sessionInfo: SessionInfo = parseSessionInfo(parsed)
                        // Notify successful sign in.
                        onLoginSuccess(name, sessionInfo)
                    }

                    else -> {
                        // Generating general error. No error available to parse.
                        onLoginFailed(GigyaApiResponse(GigyaError.generalError().data))
                    }
                }
            }

            override fun onError(gigyaError: GigyaError) {
                GigyaLogger.debug("OIDCWrapper", "getToken: fail -> $gigyaError")
                if (gigyaError.localizedMessage != null) {
                    val json = gigyaError.localizedMessage
                    if (isJSONValid(json)) {
                        val parsed: Map<String, String> =
                            gson.fromJson(json, Map::class.java) as Map<String, String>
                        if (parsed.containsKey("error_uri")) {
                            val jsonError = parseErrorUri(parsed["error_uri"]!!)
                            onLoginFailed(GigyaApiResponse(jsonError))
                        } else {
                            onLoginFailed(gigyaError.localizedMessage)
                        }
                    } else {
                        onLoginFailed(gigyaError.localizedMessage)
                    }
                } else {
                    // Generating general error. No error available to parse
                    onLoginFailed(GigyaApiResponse(GigyaError.generalError().data))
                }
            }
        })
    }

    /**
     * Parse the SessionInfo object from code exchange response.
     */
    private fun parseSessionInfo(result: Map<String, Any>): SessionInfo {
        val accessToken = result["access_token"] as String
        val expiresIn: Double = result["expires_in"] as Double
        val secret = result["device_secret"] as String
        return SessionInfo(secret, accessToken, expiresIn.toLong())
    }

    /**
     * Parse error_uri from error response.
     */
    private fun parseErrorUri(uriString: String): String {
        val uri = Uri.parse(uriString)
        val queryParams: Map<String, Any> = mutableMapOf()
        UrlUtils.parseUrlParameters(queryParams, uri.query)
        val json = JSONObject()
        json.put("callId", queryParams["callId"])
        json.put("errorCode", (queryParams["error_code"] as String).toInt())
        json.put("errorDetails", queryParams["error_description"])
        return json.toString()
    }

    fun getQueryKeyValueMap(uri: Uri): HashMap<String, Any> {
        val keyValueMap = HashMap<String, Any>()
        var key: String
        var value: String

        val keyNamesList = uri.queryParameterNames
        val iterator = keyNamesList.iterator()

        while (iterator.hasNext()) {
            key = iterator.next() as String
            value = uri.getQueryParameter(key) as String
            keyValueMap[key] = value
        }
        return keyValueMap
    }

    /**
     * Check for valid JSON String.
     */
    fun isJSONValid(test: String?): Boolean {
        try {
            JSONObject(test)
        } catch (ex: JSONException) {
            try {
                JSONArray(test)
            } catch (ex1: JSONException) {
                return false
            }
        }
        return true
    }
}