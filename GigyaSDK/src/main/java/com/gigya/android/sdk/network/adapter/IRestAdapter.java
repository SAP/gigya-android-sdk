package com.gigya.android.sdk.network.adapter;

import com.gigya.android.sdk.network.GigyaApiRequest;

public interface IRestAdapter {

    void send(GigyaApiRequest apiRequest, boolean blocking, INetworkCallbacks requestCallbacks);

    void sendBlocking(GigyaApiRequest apiRequest, INetworkCallbacks restRequestCallback);

    void block();

    void release();

    void cancel(String tag);

    String getProviderType();
}
