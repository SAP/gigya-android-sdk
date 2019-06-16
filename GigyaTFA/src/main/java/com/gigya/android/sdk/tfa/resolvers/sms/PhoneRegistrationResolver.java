package com.gigya.android.sdk.tfa.resolvers.sms;

import android.support.annotation.NonNull;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.interruption.Resolver;
import com.gigya.android.sdk.interruption.tfa.models.TFAInitModel;
import com.gigya.android.sdk.interruption.tfa.models.TFAVerificationCodeModel;
import com.gigya.android.sdk.network.GigyaError;

public class PhoneRegistrationResolver<A extends GigyaAccount> extends Resolver<A> implements IPhoneRegistrationResolver {


    public static final String LOG_TAG = "SmsRegistrationResolver";

    public PhoneRegistrationResolver(IBusinessApiService<A> businessApiService) {
        super(businessApiService);
    }

    @Override
    public void register(@NonNull String regToken, @NonNull String phoneNumber, @NonNull final RegistrationCallback registrationCallback) {
        register(regToken, phoneNumber, "sms", registrationCallback);
    }

    @Override
    public void register(final @NonNull String regToken, @NonNull final String phoneNumber, @NonNull final String method,
                         @NonNull final RegistrationCallback registrationCallback) {
        GigyaLogger.debug(LOG_TAG, "register with phone number: " + phoneNumber + " and method: " + method);
        // Start flow with init TFA method call.
        _businessApiService.initTFA(regToken, GigyaDefinitions.TFA.PHONE, "register", new GigyaCallback<TFAInitModel>() {

            @Override
            public void onSuccess(TFAInitModel model) {
                final String gigyaAssertion = model.getGigyaAssertion();
                sendVerificationCode(regToken, gigyaAssertion, phoneNumber, method, registrationCallback);
            }

            @Override
            public void onError(GigyaError error) {
                registrationCallback.onError(error);
            }
        });
    }

    private void sendVerificationCode(@NonNull final String regToken, @NonNull final String gigyaAssertion,
                                      @NonNull final String phoneNumber, @NonNull final String method, @NonNull final RegistrationCallback registrationCallback) {
        _businessApiService.registerPhoneNumber(gigyaAssertion, phoneNumber, method, new GigyaLoginCallback<TFAVerificationCodeModel>() {
            @Override
            public void onSuccess(TFAVerificationCodeModel model) {
                registrationCallback.onVerificationCodeSent(regToken, gigyaAssertion, model.getPhvToken());
            }

            @Override
            public void onError(GigyaError error) {
                registrationCallback.onError(error);
            }
        });
    }


    public interface RegistrationCallback {

        void onVerificationCodeSent(String regToken, String gigyaAssertion, String phvToken);

        void onError(GigyaError error);
    }

}
