package com.gigya.android.sdk.api;

import com.gigya.android.sdk.api.GigyaApiRequest;

import java.util.Map;

public interface IApiRequestFactory {
    GigyaApiRequest create(String api, Map<String, Object> params, int requestMethod);
}
