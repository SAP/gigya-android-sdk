package com.gigya.android.sdk.network;

import java.util.Map;

public interface IApiRequestFactory {
    GigyaApiRequest create(String api, Map<String, Object> params, int requestMethod);
}
