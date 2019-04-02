package com.gigya.android.sdk.managers;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.network.GigyaApiResponse;

import java.util.Map;

public interface IApiService<R> {

    void send(String api, Map<String, Object> params, int requestMethod, final GigyaCallback<GigyaApiResponse> callback);

    void send(String api, Map<String, Object> params, Class<R> scheme, final GigyaCallback<R> callback);

    void getConfig(final GigyaCallback<R> callback);
}
