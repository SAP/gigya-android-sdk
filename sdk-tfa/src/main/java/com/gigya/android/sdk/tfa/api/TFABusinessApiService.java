package com.gigya.android.sdk.tfa.api;

import androidx.annotation.NonNull;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.api.BusinessApiService;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IApiRequestFactory;
import com.gigya.android.sdk.api.IApiService;
import com.gigya.android.sdk.interruption.IInterruptionResolverFactory;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.providers.IProviderFactory;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.tfa.GigyaDefinitions;
import com.gigya.android.sdk.tfa.models.CompleteVerificationModel;
import com.gigya.android.sdk.tfa.models.InitTFAModel;

import java.util.HashMap;
import java.util.Map;

public class TFABusinessApiService extends BusinessApiService implements ITFABusinessApiService {

    private static final String LOG_TAG = "TFABusinessApiService";

    @SuppressWarnings("unchecked")
    public TFABusinessApiService(ISessionService sessionService,
                                 IAccountService accountService,
                                 IApiService apiService,
                                 IApiRequestFactory requestFactory,
                                 IProviderFactory providerFactory,
                                 IInterruptionResolverFactory interruptionsHandler) {
        super(sessionService, accountService, apiService, requestFactory, providerFactory, interruptionsHandler);
    }

    @Override
    public void optIntoPush(@NonNull final String deviceInfo, @NonNull final GigyaCallback<GigyaApiResponse> gigyaCallback) {
        if (!_sessionService.isValid()) {
            GigyaLogger.error(LOG_TAG, "optIntoPush: session is invalid");
            gigyaCallback.onError(GigyaError.unauthorizedUser());
            return;
        }

        // Initialize TFA first.
        final Map<String, Object> params = new HashMap<>();
        params.put("provider", GigyaDefinitions.TFAProvider.PUSH);
        params.put("mode", "register");
        send(GigyaDefinitions.API.API_TFA_INIT, params, RestAdapter.POST,
                InitTFAModel.class, new GigyaCallback<InitTFAModel>() {

                    @Override
                    public void onSuccess(InitTFAModel model) {
                        final String gigyaAssertion = model.getGigyaAssertion();
                        params.clear();
                        params.put("gigyaAssertion", gigyaAssertion);
                        params.put("deviceInfo", deviceInfo);
                        send(GigyaDefinitions.API.API_TFA_PUSH_OPT_IN, params, RestAdapter.POST,
                                GigyaApiResponse.class, gigyaCallback);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        gigyaCallback.onError(error);
                    }
                });
    }

    @Override
    public void finalizePushOptIn(@NonNull final String gigyaAssertion, @NonNull String verificationToken, @NonNull final GigyaCallback<GigyaApiResponse> gigyaCallback) {
        if (!_sessionService.isValid()) {
            GigyaLogger.error(LOG_TAG, "finalizePushOptIn: session is invalid");
            gigyaCallback.onError(GigyaError.unauthorizedUser());
            return;
        }

        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", gigyaAssertion);
        params.put("verificationToken", verificationToken);
        send(GigyaDefinitions.API.API_TFA_PUSH_VERIFY, params, RestAdapter.POST, CompleteVerificationModel.class, new GigyaCallback<CompleteVerificationModel>() {
            @Override
            public void onSuccess(CompleteVerificationModel model) {
                final String providerAssertion = model.getProviderAssertion();
                params.clear();
                params.put("gigyaAssertion", gigyaAssertion);
                params.put("providerAssertion", providerAssertion);
                send(GigyaDefinitions.API.API_TFA_FINALIZE, params, RestAdapter.POST, GigyaApiResponse.class, gigyaCallback);
            }

            @Override
            public void onError(GigyaError error) {
                gigyaCallback.onError(error);
            }
        });
    }

    @Override
    public void verifyPush(@NonNull String gigyaAssertion, @NonNull String verificationToken, @NonNull GigyaCallback<GigyaApiResponse> gigyaCallback) {
        if (!_sessionService.isValid()) {
            GigyaLogger.error(LOG_TAG, "verifyPush: session is invalid");
            gigyaCallback.onError(GigyaError.unauthorizedUser());
            return;
        }

        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", gigyaAssertion);
        params.put("verificationToken", verificationToken);
        send(GigyaDefinitions.API.API_TFA_PUSH_VERIFY, params, RestAdapter.POST, GigyaApiResponse.class, gigyaCallback);
    }
}
