package com.gigya.android.sdk.api;

import com.gigya.android.sdk.network.adapter.RestAdapter;

import java.util.Map;

public interface IApiRequestFactory {
    GigyaApiRequest create(String api, Map<String, Object> params, RestAdapter.HttpMethod httpMethod);

    GigyaApiHttpRequest sign(GigyaApiRequest request);

    GigyaApiHttpRequest unsigned(GigyaApiRequest request);
}
