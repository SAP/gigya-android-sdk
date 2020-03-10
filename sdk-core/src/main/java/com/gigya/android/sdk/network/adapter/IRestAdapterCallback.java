package com.gigya.android.sdk.network.adapter;

import com.gigya.android.sdk.network.GigyaError;

public abstract class IRestAdapterCallback {

    public abstract void onResponse(String jsonResponse, String responseDateHeader);

    public abstract void onError(GigyaError gigyaError);
}
