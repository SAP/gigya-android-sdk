package com.gigya.android.sdk.api;

import java.util.Map;

public interface IApiRequestFactory {
    GigyaApiRequest create(String api, Map<String, Object> params, int requestMethod);

    void sign(GigyaApiRequest request);
}
