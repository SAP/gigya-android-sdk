package com.gigya.android.sdk.network.adapter;

import android.content.Context;

import com.gigya.android.sdk.api.GigyaApiRequest;
import com.gigya.android.sdk.api.IApiRequestFactory;

public class RestAdapter implements IRestAdapter {

    public enum HttpMethod {

        GET(0),
        POST(1);

        private final int method;

        HttpMethod(int method) {
            this.method = method;
        }

        public int intValue() {
            return method;
        }

        public static HttpMethod fromInt(int method) {
            if (method == 1) {
                return POST;
            }
            return GET;
        }
    }

    public static final int GET = HttpMethod.GET.intValue();
    public static final int POST = HttpMethod.POST.intValue();

    private NetworkProvider _networkProvider;

    public RestAdapter(Context context, IApiRequestFactory requestFactory) {
        if (VolleyNetworkProvider.isAvailable()) {
            _networkProvider = new VolleyNetworkProvider(requestFactory, context);
        } else {
            _networkProvider = new HttpNetworkProvider(requestFactory);
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
    public void sendUnsigned(GigyaApiRequest apiRequest, IRestAdapterCallback requestCallbacks) {
        _networkProvider.addToQueueUnsigned(apiRequest, requestCallbacks);
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
