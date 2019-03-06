package com.gigya.android.sdk.api.bloc;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.services.ApiService;

import java.lang.ref.SoftReference;

public abstract class GigyaResolver<A extends GigyaAccount> {

    private static final String LOG_TAG = "GigyaResolver";

    protected final ApiService<A> _apiService;

    protected final SoftReference<GigyaLoginCallback<? extends GigyaAccount>> _loginCallback;

    protected final GigyaApiResponse _originalResponse;

    protected final String _regToken;

    public GigyaResolver(ApiService<A> apiService, GigyaApiResponse originalResponse, GigyaLoginCallback<? extends GigyaAccount> loginCallback) {
        _apiService = apiService;
        _originalResponse = originalResponse;
        _regToken = originalResponse.getField("regToken", String.class);
        _loginCallback = new SoftReference<GigyaLoginCallback<? extends GigyaAccount>>(loginCallback);
    }

    protected abstract void init();

    public abstract void clear();

    protected boolean checkCallback() {
        if (_loginCallback.get() != null) {
            return true;
        }
        GigyaLogger.debug(LOG_TAG, "checkCallback: GigyaLoginCallback reference is null -> resolver flow broken");
        return false;
    }

    protected void forwardError(GigyaError error) {
        if (_loginCallback.get() != null)
            _loginCallback.get().onError(error);
    }
}
