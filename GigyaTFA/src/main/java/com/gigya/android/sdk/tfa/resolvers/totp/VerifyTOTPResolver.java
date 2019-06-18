package com.gigya.android.sdk.tfa.resolvers.totp;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.interruption.tfa.TFAResolver;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.tfa.GigyaDefinitions;
import com.gigya.android.sdk.tfa.models.CompleteVerificationModel;
import com.gigya.android.sdk.tfa.models.InitTFAModel;

import java.util.HashMap;
import java.util.Map;

public class VerifyTOTPResolver<A extends GigyaAccount> extends TFAResolver<A> implements IVerifyTOTPResolver {

    private static final String LOG_TAG = "VerifyTOTPResolver";

    @Nullable
    private String _sctToken;

    public VerifyTOTPResolver(GigyaLoginCallback<A> loginCallback,
                              GigyaApiResponse interruption,
                              IBusinessApiService<A> businessApiService) {
        super(loginCallback, interruption, businessApiService);
    }

    VerifyTOTPResolver withAssertionAndSctToken(String gigyaAssertion, String sctToken) {
        _gigyaAssertion = gigyaAssertion;
        _sctToken = sctToken;
        return this;
    }

    @Override
    public void verifyTOTPCode(@NonNull String verificationCode, boolean rememberDevice, @NonNull ResultCallback resultCallback) {
        GigyaLogger.debug(LOG_TAG, "verifyTOTPCode with verification code: " + verificationCode);

        // Flow varies according to sctToken field availability.
        if (_sctToken == null) {
            // Start a brand new TFA initialization process.
            restartVerificationFlow(verificationCode, rememberDevice, resultCallback);
        } else {
            // Complete verification.
            completeVerification(verificationCode, rememberDevice, resultCallback);
        }
    }

    private void restartVerificationFlow(@NonNull final String verificationCode, final boolean rememeberDevice, @NonNull final ResultCallback resultCallback) {
        GigyaLogger.debug(LOG_TAG, "restartVerificationFlow: ");

        // Initialize the TFA flow.
        final Map<String, Object> params = new HashMap<>();
        params.put("regToken", getRegToken());
        params.put("provider", GigyaDefinitions.TFAProvider.TOTP);
        params.put("mode", "verify");
        _businessApiService.send(GigyaDefinitions.API.API_TFA_INIT, params, RestAdapter.POST,
                InitTFAModel.class, new GigyaCallback<InitTFAModel>() {
                    @Override
                    public void onSuccess(InitTFAModel model) {
                        _gigyaAssertion = model.getGigyaAssertion();
                        completeVerification(verificationCode, rememeberDevice, resultCallback);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        resultCallback.onError(error);
                    }
                });
    }

    private void completeVerification(@NonNull final String verificationCode, final boolean rememberDevice, @NonNull final ResultCallback resultCallback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", _gigyaAssertion);
        params.put("code", verificationCode);
        if (_sctToken != null) {
            params.put("sctToken", _sctToken);
        }
        _businessApiService.send(GigyaDefinitions.API.API_TFA_TOTP_VERIFY, params, RestAdapter.POST,
                CompleteVerificationModel.class, new GigyaCallback<CompleteVerificationModel>() {
                    @Override
                    public void onSuccess(CompleteVerificationModel model) {
                        final String providerAssertion = model.getProviderAssertion();
                        finalizeTFA(providerAssertion, rememberDevice, resultCallback);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        if (error.getErrorCode() == GigyaError.Codes.ERROR_INVALID_JWT) {
                            resultCallback.onInvalidCode();
                            return;
                        }
                        resultCallback.onError(error);
                    }
                });
    }

    private void finalizeTFA(String providerAssertion, final boolean rememberDevice, @NonNull final ResultCallback resultCallback) {
        // Finalizing the TFA flow.
        final Map<String, Object> params = new HashMap<>();
        params.put("regToken", getRegToken());
        params.put("gigyaAssertion", _gigyaAssertion);
        params.put("providerAssertion", providerAssertion);
        if (rememberDevice) {
            params.put("tempDevice", false);
        }
        _businessApiService.send(GigyaDefinitions.API.API_TFA_FINALIZE, params, RestAdapter.POST,
                GigyaApiResponse.class, new GigyaCallback<GigyaApiResponse>() {
                    @Override
                    public void onSuccess(GigyaApiResponse response) {
                        finalizeRegistration(new Runnable() {
                            @Override
                            public void run() {
                                resultCallback.onResolved();
                            }
                        });
                    }

                    @Override
                    public void onError(GigyaError error) {
                        resultCallback.onError(error);
                    }
                });
    }

    public interface ResultCallback {

        void onResolved();

        void onInvalidCode();

        void onError(GigyaError error);
    }

}
