package com.gigya.android.sdk.network.adapter;

import com.gigya.android.sdk.network.GigyaRequest;

abstract class NetworkProvider {

    boolean _blocked = false;

    abstract void addToQueue(GigyaRequest request, INetworkCallbacks networkCallbacks);

    abstract void sendBlocking(GigyaRequest request, INetworkCallbacks networkCallbacks);

    void block() {
        _blocked = true;
    }

    void release() {
        _blocked = false;
    }

    abstract void cancel(String tag);
}
