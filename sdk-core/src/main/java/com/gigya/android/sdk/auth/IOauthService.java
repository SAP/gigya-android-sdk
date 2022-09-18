package com.gigya.android.sdk.auth;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.api.ApiService;
import com.gigya.android.sdk.api.GigyaApiResponse;

public interface IOauthService {

    void connect(String token, GigyaCallback<GigyaApiResponse> callback);

    void authorize(String token, GigyaCallback<GigyaApiResponse> callback);

    void token(String token, GigyaCallback<GigyaApiResponse> callback);
}
