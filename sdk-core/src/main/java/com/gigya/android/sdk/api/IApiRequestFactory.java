package com.gigya.android.sdk.api;

import com.gigya.android.sdk.network.adapter.RestAdapter;

import java.util.HashMap;
import java.util.Map;

public interface IApiRequestFactory {
    GigyaApiRequest create(String api, Map<String, Object> params);

    GigyaApiRequest create(String api, Map<String, Object> params, RestAdapter.HttpMethod httpMethod);

    GigyaApiRequest create(String api, Map<String, Object> params, RestAdapter.HttpMethod httpMethod, HashMap<String, String> headers);

    GigyaApiHttpRequest sign(GigyaApiRequest request);

    GigyaApiHttpRequest unsigned(GigyaApiRequest request);

    void setSDK(String sdk);
}
