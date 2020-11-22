package com.gigya.android.sdk.tfa.resolvers.phone;

import androidx.annotation.NonNull;

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
import com.gigya.android.sdk.tfa.models.GetRegisteredPhoneNumbersModel;
import com.gigya.android.sdk.tfa.models.InitTFAModel;
import com.gigya.android.sdk.tfa.models.RegisteredPhone;
import com.gigya.android.sdk.tfa.models.VerificationCodeModel;
import com.gigya.android.sdk.tfa.resolvers.IVerifyCodeResolver;
import com.gigya.android.sdk.tfa.resolvers.VerifyCodeResolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolver used for TFA phone verification.
 *
 * @param <A>
 */
public class RegisteredPhonesResolver<A extends GigyaAccount> extends TFAResolver<A> implements IRegisteredPhonesResolver {

    private static final String LOG_TAG = "RegisteredPhonesResolver";

    @NonNull
    private final VerifyCodeResolver<A> _verifyCodeResolver;

    @NonNull
    private String _provider = GigyaDefinitions.TFAProvider.PHONE; // If "liveLink" will be supported we will need to update the provider.

    @NonNull
    public String getProvider() {
        return _provider;
    }

    public RegisteredPhonesResolver(GigyaLoginCallback<A> loginCallback,
                                    GigyaApiResponse interruption,
                                    IBusinessApiService<A> businessApiService,
                                    VerifyCodeResolver<A> verifyCodeResolver) {
        super(loginCallback, interruption, businessApiService);
        _verifyCodeResolver = verifyCodeResolver;
    }

    /**
     * Update the current phone provider using the builder method.
     *
     * @param provider TFA provider.
     * @return Current instance of the resolver.
     * @see com.gigya.android.sdk.tfa.GigyaDefinitions.TFAProvider.Provider for available providers.
     */
    public RegisteredPhonesResolver provider(@GigyaDefinitions.TFAProvider.Provider String provider) {
        _provider = provider;
        return this;
    }

    /**
     * Request TFA registered phone numbers.
     *
     * @param resultCallback Result callback.
     */
    @Override
    public void getPhoneNumbers(@NonNull final RegisteredPhonesResolver.ResultCallback resultCallback) {
        GigyaLogger.debug(LOG_TAG, "getPhoneNumbers: ");

        // Initialize the TFA flow.
        final Map<String, Object> params = new HashMap<>();
        params.put("regToken", getRegToken());
        params.put("provider", _provider);
        params.put("mode", "verify");
        _businessApiService.send(GigyaDefinitions.API.API_TFA_INIT, params, RestAdapter.POST, InitTFAModel.class, new GigyaCallback<InitTFAModel>() {

            @Override
            public void onSuccess(InitTFAModel model) {
                _gigyaAssertion = model.getGigyaAssertion();
                if (_gigyaAssertion == null) {
                    resultCallback.onError(GigyaError.unauthorizedUser());
                    return;
                }
                fetchNumbers(resultCallback);
            }

            @Override
            public void onError(GigyaError error) {
                resultCallback.onError(error);
            }
        });
    }

    /*
    Get registered phone numbers.
     */
    private void fetchNumbers(@NonNull final RegisteredPhonesResolver.ResultCallback resultCallback) {
        // Fetch actual phone numbers after TFA initialization.
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", _gigyaAssertion);
        _businessApiService.send(GigyaDefinitions.API.API_TFA_PHONE_GET_REGISTERED_NUMBERS, params, RestAdapter.GET,
                GetRegisteredPhoneNumbersModel.class, new GigyaCallback<GetRegisteredPhoneNumbersModel>() {
                    @Override
                    public void onSuccess(GetRegisteredPhoneNumbersModel model) {
                        final List<RegisteredPhone> registeredPhoneList = model.getPhones();
                        resultCallback.onRegisteredPhones(registeredPhoneList);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        resultCallback.onError(error);
                    }
                });
    }

    /**
     * Send TFA verification code for approval.
     * Default language is "en".
     *
     * @param phoneId        Phone number id.
     * @param method         Verification method used.
     * @param resultCallback Result callback.
     */
    @Override
    public void sendVerificationCode(@NonNull String phoneId, @NonNull @GigyaDefinitions.PhoneMethod.Method String method, @NonNull final ResultCallback resultCallback) {
        sendVerificationCode(phoneId, "en", method, resultCallback);
    }

    /**
     * Send TFA verification code for approval.
     *
     * @param phoneId        Phone number id.
     * @param lang           The language of the text or voice message.
     * @param method         Verification method used.
     * @param resultCallback Result callback.
     */
    @Override
    public void sendVerificationCode(@NonNull String phoneId, @NonNull String lang, @NonNull final String method, @NonNull final ResultCallback resultCallback) {
        GigyaLogger.debug(LOG_TAG, "sendVerificationCode with phoneId: " + phoneId + ", method: " + method);

        // Send verification code.
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", _gigyaAssertion);
        params.put("phoneID", phoneId);
        params.put("method", method);
        params.put("lang", lang);
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

        void onRegisteredPhones(List<RegisteredPhone> registeredPhoneList);

        void onVerificationCodeSent(IVerifyCodeResolver verifyCodeResolver);

        void onError(GigyaError error);
    }

}
