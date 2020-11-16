package com.gigya.android.sdk.nss.bloc.data

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.nss.utils.NssJsonDeserializer
import com.gigya.android.sdk.nss.utils.serialize
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.lang.Exception

@SuppressLint("SetJavaScriptEnabled")
class NssJsEvaluator(context: Context) {

    private var webView: WebView? = WebView(context)

    var gson: Gson = GsonBuilder().registerTypeAdapter(object : TypeToken<Map<String?, Any?>?>() {}.type, NssJsonDeserializer()).create()

    init {
        webView?.settings?.javaScriptEnabled = true
    }

    /**
     * Arrange context data.
     */
    private fun makeData(data: Map<String, Any>?): String? {
        data?.let {
            return data.map { entry ->
                "var ${entry.key} = ${gson.toJson(entry.value)}"
            }.joinToString(separator = ";") { it }
        }
        return null
    }

    /**
     * Arrange evaluation expression.
     */
    private fun makeExpressions(data: Map<String, Any>): String {
        val result = data.map { entry ->
            "${entry.key} : (${entry.value}).toString()"
        }.joinToString()
        return "{$result}"
    }

    /**
     * Evaluate conditional expression using a WebView instance.
     */
    fun eval(data: Map<String, Any>?, expressions: Map<String, Any>, result: (String) -> Unit) {
        try {
            val jsData = makeData(data)
            jsData?.let {
                webView!!.evaluateJavascript(it, null)
            }

            val jsExp = makeExpressions(expressions)
            webView!!.evaluateJavascript("JSON.stringify($jsExp);") { evalResult ->
                if (evalResult == "\"{}\"" || evalResult == "null") {
                    result("")
                    return@evaluateJavascript
                }
                val trimmed = trimJsResult(evalResult)
                result(trimmed)
            }
        } catch (ex: Exception) {
            GigyaLogger.error("NssJsEvaluator", "NssEvaluator exception")
            ex.printStackTrace()
            result("")
        }
    }

    /**
     * Trimming double stringify JS response.
     */
    private fun trimJsResult(js: String): String = gson.fromJson(js, String::class.java)

    fun mapExpressions(exp: String): Map<String, Any> {
        return when (exp.isEmpty()) {
            true -> mapOf()
            else -> exp.serialize(gson)
        }
    }

    fun dispose() {
        webView?.clearCache(true)
        webView?.destroy()
        webView = null
    }
}