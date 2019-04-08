package com.gigya.android.sdk.network.adapter;

import com.gigya.android.sdk.network.GigyaApiRequest;

public abstract class NetworkProvider {

    boolean _blocked = false;

    abstract void addToQueue(GigyaApiRequest request, IRestAdapterCallback networkCallbacks);

    abstract void sendBlocking(GigyaApiRequest request, IRestAdapterCallback networkCallbacks);

    void block() {
        _blocked = true;
    }

    void release() {
        _blocked = false;
    }

    abstract void cancel(String tag);
}
