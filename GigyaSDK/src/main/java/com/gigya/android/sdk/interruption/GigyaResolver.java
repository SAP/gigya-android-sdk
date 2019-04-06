package com.gigya.android.sdk.interruption;

import android.support.annotation.StringDef;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.services.IApiService;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.SoftReference;

public abstract class GigyaResolver<A extends GigyaAccount> implements IGigyaResolver {

    private static final String LOG_TAG = "GigyaResolver";

    protected SoftReference<GigyaLoginCallback<A>> _loginCallback;

    protected GigyaApiResponse _originalResponse;

    protected IApiService _apiService;

    String _regToken;

    public GigyaResolver(IApiService apiService, GigyaApiResponse originalResponse, GigyaLoginCallback<A> loginCallback) {
        _apiService = apiService;
        _regToken = originalResponse.getField("regToken", String.class);
        _loginCallback = new SoftReference<>(loginCallback);
    }

    /**
     * Is resolver attached to the LoginCallback reference.
     *
     * @return True if login callback reference is attached.
     */
    @Override
    public boolean isAttached() {
        if (_loginCallback != null && _loginCallback.get() != null) {
            return true;
        }
        GigyaLogger.error(LOG_TAG, "isAttached: GigyaLoginCallback reference is null -> resolver flow broken");
        return false;
    }

    void forwardError(GigyaError error) {
        if (isAttached()) {
            _loginCallback.get().onError(error);
            // Forwarding an error must result in clearing the login callback reference.
            // Login/Register flow will have to restart.
            clear();
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({TFA_REG, TFA_VER, LINK_ACCOUNTS})
    public @interface ResolverType {

    }

    public static final String TFA_REG = "tfa_registration_resolver";
    public static final String TFA_VER = "tfa_verification_resolver";
    public static final String LINK_ACCOUNTS = "link_accounts_resolver";
}
