package com.gigya.android.sdk.interruption;

import com.gigya.android.sdk.ApiManager;
import com.gigya.android.sdk.DependencyRegistry;
import com.gigya.android.sdk.GigyaLoginCallback;

public abstract class LoginResolver {

    protected GigyaLoginCallback loginCallback;
    protected ApiManager apiManager;

    public void inject(ApiManager apiManager) {
        this.apiManager = apiManager;
    }

    LoginResolver(GigyaLoginCallback loginCallback) {
        this.loginCallback = loginCallback;
        DependencyRegistry.getInstance().inject(this);
    }
}
