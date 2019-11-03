package com.gigya.android.sdk.network.adapter;

import com.gigya.android.sdk.api.GigyaApiRequest;
import com.gigya.android.sdk.api.IApiRequestFactory;

public abstract class NetworkProvider {

    public IApiRequestFactory _requestFactory;

    public NetworkProvider(IApiRequestFactory requestFactory) {
        _requestFactory = requestFactory;
    }

    public boolean _blocked = false;

    public abstract void addToQueue(GigyaApiRequest request, IRestAdapterCallback networkCallbacks);

    public abstract void sendBlocking(GigyaApiRequest request, IRestAdapterCallback networkCallbacks);

    public void block() {
        _blocked = true;
    }

    public void release() {
        _blocked = false;
    }

    public abstract void cancel(String tag);
}
