package com.gigya.android.sdk.auth;

import com.gigya.android.sdk.api.ApiService;

public interface IOauthService {

    void connect(String token, ApiService.IApiServiceResponse iApiServiceResponse);

    void authorize(String token, ApiService.IApiServiceResponse iApiServiceResponse);

    void token(String code, ApiService.IApiServiceResponse iApiServiceResponse);
}
