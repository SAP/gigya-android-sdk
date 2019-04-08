package com.gigya.android.sdk.network.adapter;

import android.content.Context;

import com.gigya.android.sdk.network.GigyaApiRequest;

public class RestAdapter implements IRestAdapter {

    public static final int GET = 0;
    public static final int POST = 1;

    private NetworkProvider _networkProvider;

    public RestAdapter(Context context) {
        if (VolleyNetworkProvider.isAvailable()) {
            _networkProvider = new VolleyNetworkProvider(context);
        } else {
            _networkProvider = new HttpNetworkProvider();
        }
    }

    @Override
    public void send(GigyaApiRequest apiRequest, boolean blocking, IRestAdapterCallback requestCallbacks) {
        if (blocking) {
            sendBlocking(apiRequest, requestCallbacks);
            return;
        }
        _networkProvider.addToQueue(apiRequest, requestCallbacks);
    }

    @Override
    public void sendBlocking(GigyaApiRequest apiRequest, IRestAdapterCallback requestCallbacks) {
        _networkProvider.sendBlocking(apiRequest, requestCallbacks);
    }

    @Override
    public void block() {
        _networkProvider.block();
    }

    @Override
    public void release() {
        _networkProvider.release();
    }

    @Override
    public void cancel(String tag) {
        _networkProvider.cancel(tag);
    }

    @Override
    public String getProviderType() {
        try {
            return _networkProvider.getClass().getSimpleName();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
