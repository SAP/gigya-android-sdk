package com.gigya.android.sdk.auth.api;

import android.support.annotation.NonNull;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.api.BusinessApiService;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IApiRequestFactory;
import com.gigya.android.sdk.api.IApiService;
import com.gigya.android.sdk.auth.GigyaDefinitions;
import com.gigya.android.sdk.auth.persistence.IAuthPersistenceService;
import com.gigya.android.sdk.interruption.IInterruptionResolverFactory;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.providers.IProviderFactory;
import com.gigya.android.sdk.session.ISessionService;

import java.util.HashMap;
import java.util.Map;

public class AuthBusinessApiService extends BusinessApiService implements IAuthBusinessApiService {

    private static final String LOG_TAG = "AuthBusinessApiService";

    public AuthBusinessApiService(ISessionService sessionService,
                                  IAccountService accountService,
                                  IApiService apiService,
                                  IApiRequestFactory requestFactory,
                                  IProviderFactory providerFactory,
                                  IAuthPersistenceService persistenceService,
                                  IInterruptionResolverFactory interruptionsHandler) {
        super(sessionService, accountService, apiService, requestFactory, providerFactory, persistenceService, interruptionsHandler);
    }

    @Override
    public void registerDevice(@NonNull final String deviceInfo, @NonNull final GigyaCallback<GigyaApiResponse> gigyaCallback) {
        if (!_sessionService.isValid()) {
            GigyaLogger.error(LOG_TAG, "registerDevice: session is invalid");
            gigyaCallback.onError(GigyaError.unauthorizedUser());
            return;
        }

        GigyaLogger.debug(LOG_TAG, "registerDevice: with device Info " + deviceInfo);

        final Map<String, Object> params = new HashMap<>();
        params.put("deviceInfo", deviceInfo);
        send(GigyaDefinitions.API.API_AUTH_DEVICE_REGISTER, params, RestAdapter.POST,
                GigyaApiResponse.class, new GigyaCallback<GigyaApiResponse>() {

                    @Override
                    public void onSuccess(GigyaApiResponse model) {

                        ((IAuthPersistenceService) _persistenceService).updateAuthPushState(true);

                        GigyaLogger.debug(LOG_TAG, "registerDevice: successfully registered device information");

                        gigyaCallback.onSuccess(model);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        GigyaLogger.error(LOG_TAG, "registerDevice: failed to register device information");

                        gigyaCallback.onError(error);
                    }
                });
    }

    @Override
    public void unregisterDevice(@NonNull GigyaCallback<GigyaApiResponse> gigyaCallback) {
        if (!_sessionService.isValid()) {
            GigyaLogger.error(LOG_TAG, "unregisterDevice: session is invalid");
            gigyaCallback.onError(GigyaError.unauthorizedUser());
            return;
        }

        GigyaLogger.error(LOG_TAG, "unregisterDevice: Feature currently unavailable");

        //((IAuthPersistenceService) _persistenceService).updateAuthPushState(false);
    }

    @Override
    public void verifyPush(@NonNull String vToken, @NonNull final GigyaCallback<GigyaApiResponse> gigyaCallback) {
        if (!_sessionService.isValid()) {
            GigyaLogger.error(LOG_TAG, "verifyPush: session is invalid");
            gigyaCallback.onError(GigyaError.unauthorizedUser());
            return;
        }

        GigyaLogger.debug(LOG_TAG, "verifyPush: with vToken " + vToken);

        final Map<String, Object> params = new HashMap<>();
        params.put("vToken", vToken);
        send(GigyaDefinitions.API.API_AUTH_PUSH_VERIFY, params, RestAdapter.POST,
                GigyaApiResponse.class, new GigyaCallback<GigyaApiResponse>() {

                    @Override
                    public void onSuccess(GigyaApiResponse model) {
                        GigyaLogger.debug(LOG_TAG, "verifyPush: successfully verified push authentication request");
                        gigyaCallback.onSuccess(model);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        GigyaLogger.error(LOG_TAG, "verifyPush: failed to verify push authentication request with error " + error.getErrorCode());
                        gigyaCallback.onError(error);
                    }
                });
    }
}
