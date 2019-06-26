package com.gigya.android.sdk.tfa.resolvers.phone;

import android.support.annotation.NonNull;

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
import com.gigya.android.sdk.tfa.models.InitTFAModel;
import com.gigya.android.sdk.tfa.models.VerificationCodeModel;
import com.gigya.android.sdk.tfa.resolvers.IVerifyCodeResolver;
import com.gigya.android.sdk.tfa.resolvers.VerifyCodeResolver;

import java.util.HashMap;
import java.util.Map;

import static com.gigya.android.sdk.tfa.GigyaDefinitions.PhoneMethod.SMS;

public class RegisterPhoneResolver<A extends GigyaAccount> extends TFAResolver<A> implements IRegisterPhoneResolver {

    private static final String LOG_TAG = "SmsRegistrationResolver";

    @NonNull
    private final VerifyCodeResolver<A> _verifyCodeResolver;

    @NonNull
    private String _provider = GigyaDefinitions.TFAProvider.PHONE.toString(); // If "liveLink" will be supported we will need to update the provider.

    @NonNull
    public String getProvider() {
        return _provider;
    }

    public RegisterPhoneResolver(GigyaLoginCallback<A> loginCallback,
                                 GigyaApiResponse interruption,
                                 IBusinessApiService<A> businessApiService,
                                 VerifyCodeResolver<A> verifyCodeResolver) {
        super(loginCallback, interruption, businessApiService);
        _verifyCodeResolver = verifyCodeResolver;
    }

    public RegisterPhoneResolver provider(@GigyaDefinitions.TFAProvider.Provider String provider) {
        _provider = provider;
        return this;
    }

    @Override
    public void registerPhone(@NonNull String phoneNumber, @NonNull ResultCallback resultCallback) {
        registerPhone(phoneNumber, SMS, resultCallback);
    }

    @Override
    public void registerPhone(final @NonNull String phoneNumber, final @NonNull @GigyaDefinitions.PhoneMethod.Method String method,
                              @NonNull final ResultCallback resultCallback) {
        GigyaLogger.debug(LOG_TAG, "register with phoneNumber: " + phoneNumber + ", method: " + method);

        // Initialize the TFA flow.
        final Map<String, Object> params = new HashMap<>();
        params.put("regToken", getRegToken());
        params.put("provider", _provider);
        params.put("mode", "register");
        _businessApiService.send(GigyaDefinitions.API.API_TFA_INIT, params, RestAdapter.POST, InitTFAModel.class, new GigyaCallback<InitTFAModel>() {
            @Override
            public void onSuccess(InitTFAModel model) {
                _gigyaAssertion = model.getGigyaAssertion();
                if (_gigyaAssertion == null) {
                    resultCallback.onError(GigyaError.unauthorizedUser());
                    return;
                }
                registerPhoneNumber(phoneNumber, method, resultCallback);
            }

            @Override
            public void onError(GigyaError error) {
                resultCallback.onError(error);
            }
        });
    }

    private void registerPhoneNumber(@NonNull final String phoneNumber, @NonNull @GigyaDefinitions.PhoneMethod.Method final String method,
                                     @NonNull final ResultCallback resultCallback) {
        GigyaLogger.debug(LOG_TAG, "register with phone number: " + phoneNumber + " and method: " + method);

        // Send verification code.
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", _gigyaAssertion);
        params.put("phone", phoneNumber);
        params.put("method", method);
        params.put("lang", "eng");
        _businessApiService.send(GigyaDefinitions.API.API_TFA_PHONE_SEND_VERIFICATION_CODE, params, RestAdapter.POST,
                VerificationCodeModel.class, new GigyaCallback<VerificationCodeModel>() {
                    @Override
                    public void onSuccess(VerificationCodeModel model) {
                        final String phvToken = model.getPhvToken();
                        resultCallback.onVerificationCodeSent(_verifyCodeResolver.withAssertionAndPhvToken(_gigyaAssertion, phvToken));
                    }

                    @Override
                    public void onError(GigyaError error) {
                        resultCallback.onError(error);
                    }
                });
    }

    public interface ResultCallback {

        void onVerificationCodeSent(IVerifyCodeResolver verifyCodeResolver);

        void onError(GigyaError error);
    }

}
