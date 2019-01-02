package com.gigya.android.sdk.network.adapter;

import android.content.Context;

import com.gigya.android.sdk.network.GigyaRequest;

public class NetworkAdapter {

    private NetworkProvider _networkProvider;

    public NetworkAdapter(Context appContext) {
        /*
        Currently the only available network provider.
         */
        if (VolleyNetworkProvider.isAvailable()) {
            _networkProvider = new VolleyNetworkProvider(appContext);
        }
        ///.... Add additional providers such as retrofit etc.
        else {
            _networkProvider = new HttpNetworkProvider();
        }
    }

    public void send(GigyaRequest request,
                     INetworkCallbacks networkCallbacks) {
        _networkProvider.addToQueue(request, networkCallbacks);
    }

    public void block() {
        _networkProvider.block();
    }

    public void release() {
        _networkProvider.release();
    }

    public void cancel(String tag) {
        _networkProvider.cancel(tag);
    }

    public enum Method {
        GET(0), POST(1);

        private final int value;

        Method(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
