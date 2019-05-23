package com.gigya.android.sdk.interruption;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.api.IApiObservable;
import com.gigya.android.sdk.api.IApiService;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.IApiRequestFactory;
import com.gigya.android.sdk.session.ISessionService;

import java.lang.ref.SoftReference;

public abstract class GigyaResolver<A extends GigyaAccount> implements IGigyaResolver {

    private static final String LOG_TAG = "GigyaResolver";

    // Dependencies.
    protected Config _config;
    protected ISessionService _sessionService;
    protected IApiService _apiService;
    protected IApiRequestFactory _requestFactory;

    // Resolver specific.
    protected SoftReference<GigyaLoginCallback<A>> _loginCallback;
    protected GigyaApiResponse _originalResponse;
    protected IApiObservable _observable;

    protected String _regToken;

    public GigyaResolver(Config config,
                         ISessionService sessionService,
                         IApiService apiService,
                         IApiObservable observable,
                         IApiRequestFactory requestFactory,
                         GigyaApiResponse originalResponse,
                         GigyaLoginCallback<A> loginCallback) {
        // Dependencies.
        _config = config;
        _sessionService = sessionService;
        _apiService = apiService;
        _observable = observable;
        _requestFactory = requestFactory;
        _originalResponse = originalResponse;
        // Handlers.
        _loginCallback = new SoftReference<>(loginCallback);
        _regToken = originalResponse.getField("regToken", String.class);
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

    @Override
    public void clear() {
        _observable.dispose();
    }

    public void forwardError(GigyaError error) {
        if (isAttached()) {
            _loginCallback.get().onError(error);
            // Forwarding an error must result in clearing the login callback reference.
            // Login/Register flow will have to restart.
            clear();
        }
    }
}
