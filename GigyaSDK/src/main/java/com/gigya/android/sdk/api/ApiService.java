package com.gigya.android.sdk.api;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.network.GigyaApiRequest;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.IRestAdapter;
import com.gigya.android.sdk.network.adapter.IRestAdapterCallback;

/**
 * Service responsible for mediating & executing HTTP based api requests.
 */
public class ApiService implements IApiService {

    final private IRestAdapter _adapter;

    public ApiService(IRestAdapter adapter) {
        _adapter = adapter;
    }

    @Override
    public void send(GigyaApiRequest request, boolean blocking, final IApiServiceResponse apiCallback) {
        GigyaLogger.debug("ApiService", "sending: " + request.getApi() + "\n" + request.getEncodedParams());
        _adapter.send(request, blocking, new IRestAdapterCallback() {
            @Override
            public void onResponse(String jsonResponse) {
                final GigyaApiResponse apiResponse = new GigyaApiResponse(jsonResponse);
                apiCallback.onApiSuccess(apiResponse);
            }

            @Override
            public void onError(GigyaError gigyaError) {
                apiCallback.onApiError(gigyaError);
            }
        });
    }

    @Override
    public void release() {
        _adapter.release();
    }

    @Override
    public void cancel(String tag) {
        _adapter.cancel(tag);
    }

    public interface IApiServiceResponse {

        void onApiSuccess(GigyaApiResponse response);

        void onApiError(GigyaError gigyaError);
    }
}
