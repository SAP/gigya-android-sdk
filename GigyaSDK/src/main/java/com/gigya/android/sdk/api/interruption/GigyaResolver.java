package com.gigya.android.sdk.api.interruption;

import android.support.annotation.StringDef;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.services.ApiService;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.SoftReference;

public abstract class GigyaResolver<A extends GigyaAccount> {

    private static final String LOG_TAG = "GigyaResolver";

    ApiService<A> _apiService;

    protected SoftReference<GigyaLoginCallback<? extends GigyaAccount>> _loginCallback;

    protected GigyaApiResponse _originalResponse;

    String _regToken;

    public void init(ApiService<A> apiService, GigyaApiResponse originalResponse, GigyaLoginCallback<? extends GigyaAccount> loginCallback) {
        _apiService = apiService;
        _originalResponse = originalResponse;
        _regToken = originalResponse.getField("regToken", String.class);
        // TODO: 10/03/2019 If regToken in null throw an error.
        _loginCallback = new SoftReference<GigyaLoginCallback<? extends GigyaAccount>>(loginCallback);
    }

    /**
     * Clear the resolver from data. Release login callback.
     */
    public abstract void clear();

    /**
     * Is resolver attached to the LoginCallback reference.
     *
     * @return True if login callback reference is attached.
     */
    public boolean isAttached() {
        if (_loginCallback.get() != null) {
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
