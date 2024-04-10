package com.gigya.android.sdk.auth;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.api.ApiService;
import com.gigya.android.sdk.api.GigyaApiResponse;

import java.util.Map;

public interface IOauthService {

    void connect(String token, GigyaCallback<GigyaApiResponse> callback);

    void disconnect(String regToken, String idToken, boolean ignoreApiQueue, GigyaCallback<GigyaApiResponse> callback);

    void authorize(String token, GigyaCallback<GigyaApiResponse> callback);

    void token(String token, GigyaCallback<GigyaApiResponse> callback);

    void setLoginParams(Map<String, Object> params);

    void clearLoginParams();
}
