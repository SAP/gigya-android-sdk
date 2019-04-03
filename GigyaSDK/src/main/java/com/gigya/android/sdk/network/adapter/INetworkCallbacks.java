package com.gigya.android.sdk.network.adapter;

import com.gigya.android.sdk.network.GigyaError;

public abstract class INetworkCallbacks {

    public abstract void onResponse(String jsonResponse);

    public abstract void onError(GigyaError gigyaError);
}
