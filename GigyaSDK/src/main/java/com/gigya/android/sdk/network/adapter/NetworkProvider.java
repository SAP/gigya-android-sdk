package com.gigya.android.sdk.network.adapter;

import com.gigya.android.sdk.api.GigyaApiRequest;
import com.gigya.android.sdk.api.IGigyaApiRequestSigner;

public abstract class NetworkProvider {

    public IGigyaApiRequestSigner _requestSigner;

    public NetworkProvider(IGigyaApiRequestSigner requestSigner) {
        _requestSigner = requestSigner;
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
