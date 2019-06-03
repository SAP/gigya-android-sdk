package com.gigya.android.sdk.network.adapter;

import com.gigya.android.sdk.api.GigyaApiRequest;

public abstract class NetworkProvider {

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
