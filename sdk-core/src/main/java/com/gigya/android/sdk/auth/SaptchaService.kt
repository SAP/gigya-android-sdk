package com.gigya.android.sdk.auth

import android.util.Base64
import android.util.Log
import com.gigya.android.sdk.GigyaCallback
import com.gigya.android.sdk.GigyaDefinitions
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.api.GigyaApiResponse
import com.gigya.android.sdk.api.IBusinessApiService
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.network.adapter.RestAdapter
import org.json.JSONObject
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

interface ISaptchaService<A : GigyaAccount> {
    fun startChallenge(callback: GigyaCallback<GigyaApiResponse>)
    fun verifyChallenge(token: String, callback: GigyaCallback<GigyaApiResponse>)
}

class SaptchaService<A : GigyaAccount>(
    private val businessApiService: IBusinessApiService<A>,
) : ISaptchaService<A> {

    private val saptchaUtils = SaptchaUtils()

    override fun startChallenge(callback: GigyaCallback<GigyaApiResponse>) {
        businessApiService.send(
            GigyaDefinitions.API.API_SAPTCHA_GET_CHALLENGE,
            mapOf(),
            RestAdapter.POST,
            GigyaApiResponse::class.java,
            object : GigyaCallback<GigyaApiResponse>() {
                override fun onSuccess(obj: GigyaApiResponse?) {
                    if (obj != null) {
                        val token = obj.getField("saptchaToken", String::class.java)
                        verifyChallenge(token!!, callback)
                    }
                }

                override fun onError(error: GigyaError?) {
                    callback.onError(error)
                }
            }
        )
    }

    override fun verifyChallenge(token: String, callback: GigyaCallback<GigyaApiResponse>) {
        val verifyAlgo = saptchaUtils.verifySaptcha(token)
        val params = mapOf("token" to "$token|$verifyAlgo")
        businessApiService.send(
            GigyaDefinitions.API.API_SAPTCHA_VERIFY,
            params,
            RestAdapter.POST,
            GigyaApiResponse::class.java,
            object : GigyaCallback<GigyaApiResponse>() {
                override fun onSuccess(obj: GigyaApiResponse?) {
                    callback.onSuccess(obj)
                }

                override fun onError(error: GigyaError?) {
                    callback.onError(error)
                }
            }
        )
    }
}

class SaptchaUtils {

    private fun verifyChallenge(challengeId: String, pattern: String, i: Int): Boolean {
        val value = encodeWith(input = "$challengeId.$i")
        val regex = pattern.toRegex()
        GigyaLogger.debug("SaptchaUtils", "Challenge: $challengeId.$i, Value: $value")
        return regex.containsMatchIn(value)
    }

    fun verifySaptcha(jwt: String): Int {
        val jwtObject = jwt.jwtDecode()
        val jti = jwtObject.getString("jti")
        val pattern = jwtObject.getString("pattern")

        var i = 0
        var isFinished = false
        while (!isFinished) {
            i += 1
            isFinished = verifyChallenge(jti, pattern, i)
        }
        GigyaLogger.debug("SaptchaUtils", "Saptcha verification completed in $i iterations")
        return i
    }

    private fun encodeWith(input: String): String {
        val md: MessageDigest = MessageDigest.getInstance("SHA-512")
        val messageDigest = md.digest(input.toByteArray())
        val no = BigInteger(1, messageDigest)
        var hash: String = no.toString(16)
        // Add preceding 0s to make it 128 chars long
        while (hash.length < 128) {
            hash = "0$hash"
        }
        return hash
    }
}

fun String.jwtDecode(): JSONObject {
    val parts = this.split(".")
    val base64EncodedData = parts[1]
    val data = Base64.decode(
        base64EncodedData.toByteArray(charset = StandardCharsets.UTF_8),
        Base64.DEFAULT
    )
    return JSONObject(String(data, StandardCharsets.UTF_8))
}