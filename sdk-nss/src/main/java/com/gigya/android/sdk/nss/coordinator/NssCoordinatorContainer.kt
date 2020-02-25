package com.gigya.android.sdk.nss.coordinator

import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.nss.flows.NssFlow

open class NssCoordinatorContainer<T : GigyaAccount> {

    private val mFlowMap = linkedMapOf<String, NssFlow<T>>()

    private var mCurrentFlow: String? = null

    fun add(id: String, flow: NssFlow<T>) {
        mFlowMap[id] = flow
        mCurrentFlow = id
    }

    fun remove(id: String) {
        mFlowMap.remove(id)
        mCurrentFlow = mFlowMap.keys.last()
    }

    fun get(id: String): NssFlow<T>? = mFlowMap[id]

    fun getCurrent(): NssFlow<T>? = mFlowMap[mCurrentFlow]

    fun clear() {
        mFlowMap.clear()
    }
}