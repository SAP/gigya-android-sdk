package com.gigya.android.sdk.auth;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.api.ApiService;
import com.gigya.android.sdk.api.GigyaApiRequest;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IApiRequestFactory;
import com.gigya.android.sdk.api.IApiService;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.RestAdapter;

import java.util.HashMap;
import java.util.Map;

public class OauthService implements IOauthService {

    private static final String LOG_TAG = "OauthService";

    final private IApiService apiService;
    final private IApiRequestFactory requestFactory;

    public OauthService(
            IApiService apiService,
            IApiRequestFactory requestFactory
    ) {
        this.apiService = apiService;
        this.requestFactory = requestFactory;
    }

    /**
     * @param token
     */
    @Override
    public void connect(String token) {
        final HashMap<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        GigyaApiRequest request = this.requestFactory.create(
                "oauth.connect", null, RestAdapter.HttpMethod.POST, headers
        );
        this.apiService.send(request, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                GigyaLogger.debug(LOG_TAG, "connect api success response:\n" + response.asJson());
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                GigyaLogger.debug(LOG_TAG, "connect api error: \n" + gigyaError.getData());
            }
        });

    }

    /**
     * @param token
     */
    @Override
    public void authorize(String token) {
        final Map<String, Object> params = new HashMap<>();
        params.put("response_type", "code");
        final HashMap<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        GigyaApiRequest request = this.requestFactory.create(
                "oauth.authorize", params, RestAdapter.HttpMethod.POST, headers
        );
        this.apiService.send(request, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                GigyaLogger.debug(LOG_TAG, "authorize api success response:\n" + response.asJson());

                if (response.contains("code")) {
                    final String code = response.getField("code", String.class);
                    token(code);
                }
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                GigyaLogger.debug(LOG_TAG, "authorize api error: \n" + gigyaError.getData());
            }
        });
    }

    /**
     * @param code
     */
    @Override
    public void token(String code) {
        final Map<String, Object> params = new HashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("code", code);
        GigyaApiRequest request = this.requestFactory.create(
                "oauth.connect", params, RestAdapter.HttpMethod.POST
        );
        this.apiService.send(request, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                GigyaLogger.debug(LOG_TAG, "token api success response:\n" + response.asJson());
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                GigyaLogger.debug(LOG_TAG, "token api error: \n" + gigyaError.getData());
            }
        });
    }

}


