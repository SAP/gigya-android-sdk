package com.gigya.android.sdk.interruption;

import com.gigya.android.sdk.ApiManager;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.network.GigyaResponse;

public abstract class LoginResolver {

    protected GigyaLoginCallback loginCallback;
    protected ApiManager apiManager;
    protected GigyaResponse gigyaResponse;

    LoginResolver(GigyaResponse response, GigyaLoginCallback loginCallback) {
        this.gigyaResponse = response;
        this.loginCallback = loginCallback;
    }
}
