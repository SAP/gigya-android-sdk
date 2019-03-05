package com.gigya.android.sdk.network.adapter;

import com.gigya.android.sdk.network.GigyaApiRequest;

abstract class NetworkProvider {

    boolean _blocked = false;

    abstract void addToQueue(GigyaApiRequest request, INetworkCallbacks networkCallbacks);

    abstract void sendBlocking(GigyaApiRequest request, INetworkCallbacks networkCallbacks);

    void block() {
        _blocked = true;
    }

    void release() {
        _blocked = false;
    }

    abstract void cancel(String tag);
}
