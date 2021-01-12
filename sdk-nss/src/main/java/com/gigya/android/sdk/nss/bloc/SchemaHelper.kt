package com.gigya.android.sdk.nss.bloc

import com.gigya.android.sdk.GigyaCallback
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.api.GigyaApiResponse
import com.gigya.android.sdk.api.IBusinessApiService
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.network.adapter.RestAdapter
import io.flutter.plugin.common.MethodChannel

/**
 * Helper class used for site schema specific tasks.
 */
class SchemaHelper<T : GigyaAccount>(private val businessApi: IBusinessApiService<T>) {

    companion object {
        const val LOG_TAG = "SchemaHelper"
    }

    /**
     * Fetch the updated account schema.
     */
    fun getSchema(result: MethodChannel.Result) {
        businessApi.send("accounts.getSchema",
                null,
                RestAdapter.HttpMethod.POST.intValue(),
                GigyaApiResponse::class.java,
                object : GigyaCallback<GigyaApiResponse>() {
                    override fun onSuccess(obj: GigyaApiResponse?) {
                        result.success(obj?.asMap())
                    }

                    override fun onError(error: GigyaError?) {
                        GigyaLogger.error(LOG_TAG, "Failed to fetch schema.")
                        result.error(error?.errorCode.toString(), error?.localizedMessage, error?.data)
                    }
                })
    }

}