package com.gigya.android.sdk.network.adapter;

import com.gigya.android.sdk.api.GigyaApiRequest;
import com.gigya.android.sdk.api.IApiRequestFactory;

public interface IRestAdapter {

    void send(GigyaApiRequest apiRequest, boolean blocking, IRestAdapterCallback requestCallbacks);

    void sendUnsigned(GigyaApiRequest apiRequest, IRestAdapterCallback requestCallbacks);

    void sendBlocking(GigyaApiRequest apiRequest, IRestAdapterCallback restRequestCallback);

    void block();

    void release();

    void cancel(String tag);

    String getProviderType();

    void forceDefaultRestAdapter(IApiRequestFactory requestFactory);
}
