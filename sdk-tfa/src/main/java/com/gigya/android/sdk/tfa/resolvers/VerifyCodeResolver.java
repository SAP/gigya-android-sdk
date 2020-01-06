package com.gigya.android.sdk.tfa.resolvers;

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

import java.util.HashMap;
import java.util.Map;

/**
 * Two factor authentication verification resolver class.
 * This resolver is responsible for verifying two factor authentication flows.
 * Supported providers:
 * 1. gigyaPhone
 * 2. livelink
 * 3. gigyaTotp
 *
 * @param <A> Account scheme.
 */
public class VerifyCodeResolver<A extends GigyaAccount> extends TFAResolver<A> implements IVerifyCodeResolver {

    private static final String LOG_TAG = "VerifyCodeResolver";

    @Nullable
    private String _phvToken;

    public VerifyCodeResolver(GigyaLoginCallback<A> loginCallback,
                              GigyaApiResponse interruption,
                              IBusinessApiService<A> businessApiService) {
        super(loginCallback, interruption, businessApiService);
    }

    /**
     * Adds required fields "gigyaAssertion" & "phvToken" using the builder method.
     *
     * @return Current resolver instance.
     */
    public VerifyCodeResolver withAssertionAndPhvToken(String gigyaAssertion, String phvToken) {
        _gigyaAssertion = gigyaAssertion;
        _phvToken = phvToken;
        return this;
    }

    private void finalizeTFA(String providerAssertion, boolean rememberDevice, @NonNull final ResultCallback resultCallback) {
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

    private String getVerificationApi(@NonNull @GigyaDefinitions.TFAProvider.Provider String provider) {
        if (provider.equals(GigyaDefinitions.TFAProvider.PHONE) || provider.equals(GigyaDefinitions.TFAProvider.LIVELINK)) {
            return GigyaDefinitions.API.API_TFA_PHONE_COMPLETE_VERIFICATION;
        } else if (provider.equals(GigyaDefinitions.TFAProvider.EMAIL)) {
            return GigyaDefinitions.API.API_TFA_EMAIL_COMPLETE_VERIFICATION;
        }
        return "";
    }

    /**
     * Verify provided code.
     *
     * @param provider         Selected TFA provider.
     * @param verificationCode Verification code.
     * @param rememberDevice   Remember this device option.
     * @param resultCallback   Result callback.
     */
    @Override
    public void verifyCode(@NonNull @GigyaDefinitions.TFAProvider.Provider String provider, @NonNull String verificationCode, final boolean rememberDevice,
                           @NonNull final ResultCallback resultCallback) {
        GigyaLogger.debug(LOG_TAG, "verifyCode: code = " + verificationCode);
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", _gigyaAssertion);
        params.put("phvToken", _phvToken);
        params.put("code", verificationCode);
        _businessApiService.send(getVerificationApi(provider), params, RestAdapter.POST,
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

    public interface ResultCallback {

        void onResolved();

        void onInvalidCode();

        void onError(GigyaError error);
    }

}
