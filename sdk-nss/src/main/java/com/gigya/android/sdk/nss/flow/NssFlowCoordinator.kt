package com.gigya.android.sdk.nss.coordinator

import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.nss.flow.NssFlow

open class NssCoordinatorContainer<T : GigyaAccount> {

    private val mFlowMap = linkedMapOf<String, NssFlow<T>>()

    private var mCurrentFlow: String? = null

    fun addFlow(id: String, flow: NssFlow<T>) {
        mFlowMap[id] = flow
        mCurrentFlow = id
    }

    fun removeFlow(id: String) {
        mFlowMap.remove(id)
        mCurrentFlow = mFlowMap.keys.last()
    }

    fun getFlowWith(id: String): NssFlow<T>? = mFlowMap[id]

    fun getCurrentFlow(): NssFlow<T>? = mFlowMap[mCurrentFlow]

    fun clearCoordinatorContainer() {
        mFlowMap.clear()
    }
}