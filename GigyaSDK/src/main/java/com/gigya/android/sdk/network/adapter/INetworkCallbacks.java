package com.gigya.android.sdk.network.adapter;

import com.gigya.android.sdk.network.GigyaError;

public interface INetworkCallbacks {

    public void onResponse(String jsonResponse);

    public void onError(GigyaError gigyaError);
}
