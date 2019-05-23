package com.gigya.android.sdk.api;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.model.GigyaConfigModel;
import com.gigya.android.sdk.network.GigyaApiRequest;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.IApiRequestFactory;
import com.gigya.android.sdk.network.adapter.IRestAdapter;
import com.gigya.android.sdk.network.adapter.IRestAdapterCallback;
import com.gigya.android.sdk.network.adapter.RestAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * Service responsible for mediating and executing HTTP based api requests.
 */
public class ApiService implements IApiService {

    final private Config _config;
    final private IRestAdapter _adapter;
    final private IApiRequestFactory _reqFactory;

    public ApiService(Config config, IRestAdapter adapter, IApiRequestFactory reqFactory) {
        _config = config;
        _adapter = adapter;
        _reqFactory = reqFactory;
    }

    @Override
    public void send(GigyaApiRequest request, boolean blocking, final IApiServiceResponse apiCallback) {
        if (requiresSdkConfig()) {
            // Need to verify if GMID is available. If not we must request SDK configuration.
            getSdkConfig(apiCallback, request.getTag());
        }
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

    //region SDK CONFIG

    private boolean requiresSdkConfig() {
        return (_config.getGmid() == null);
    }

    private void onConfigResponse(GigyaConfigModel response) {
        _config.setUcid(response.getIds().getUcid());
        _config.setGmid(response.getIds().getGmid());
        release();
    }

    private void onConfigError(String nextApiTag) {
        if (nextApiTag != null) {
            cancel(nextApiTag);
        }
        release();
    }

    private void getSdkConfig(final IApiServiceResponse apiCallback, final String nextApiTag) {
        final Map<String, Object> params = new HashMap<>();
        params.put("include", "permissions,ids,appIds");
        final GigyaApiRequest request = _reqFactory.create(GigyaDefinitions.API.API_GET_SDK_CONFIG, params, RestAdapter.GET);
        _adapter.send(request, true, new IRestAdapterCallback() {
            @Override
            public void onResponse(String jsonResponse) {
                final GigyaApiResponse apiResponse = new GigyaApiResponse(jsonResponse);
                if (apiResponse.getErrorCode() == 0) {
                    final GigyaConfigModel parsed = apiResponse.parseTo(GigyaConfigModel.class);
                    if (parsed == null) {
                        // Parsing error.
                        apiCallback.onApiError(GigyaError.fromResponse(apiResponse));
                        onConfigError(nextApiTag);
                        return;
                    }
                    onConfigResponse(parsed);
                } else {
                    apiCallback.onApiError(GigyaError.fromResponse(apiResponse));
                    onConfigError(nextApiTag);
                }
            }

            @Override
            public void onError(GigyaError gigyaError) {
                apiCallback.onApiError(gigyaError);
                onConfigError(nextApiTag);
            }
        });
    }

    //endregion
}
