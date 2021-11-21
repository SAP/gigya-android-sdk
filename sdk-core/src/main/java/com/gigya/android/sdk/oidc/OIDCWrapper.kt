package com.gigya.android.sdk.oidc

import android.content.Context
import android.content.Intent

class OIDCWrapper(val context: Context) {

    private fun getOidcActivity(pathToActivity: String): Class<*> {
        val packageName = context.packageName
        val classPath = "$packageName.${pathToActivity}" +
                "${
                    when (pathToActivity.isEmpty()) {
                        true -> ""
                        false -> "."
                    }
                }GigyaOIDCActivity"
        return Class.forName(classPath)
    }

    fun show(pathToActivity: String) {
        val intent = Intent(context, getOidcActivity(pathToActivity))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}