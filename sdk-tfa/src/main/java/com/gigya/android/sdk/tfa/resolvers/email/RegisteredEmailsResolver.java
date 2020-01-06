package com.gigya.android.sdk.tfa.resolvers.email;

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
import com.gigya.android.sdk.tfa.models.EmailModel;
import com.gigya.android.sdk.tfa.models.GetEmailsModel;
import com.gigya.android.sdk.tfa.models.InitTFAModel;
import com.gigya.android.sdk.tfa.models.VerificationCodeModel;
import com.gigya.android.sdk.tfa.resolvers.IVerifyCodeResolver;
import com.gigya.android.sdk.tfa.resolvers.VerifyCodeResolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolver used for Email TFA registration.
 *
 * @param <A>
 */
public class RegisteredEmailsResolver<A extends GigyaAccount> extends TFAResolver<A> implements IRegisteredEmailsResolver {

    private static final String LOG_TAG = "RegisteredEmailsResolver";

    @NonNull
    private final VerifyCodeResolver _verifyCodeResolver;

    public RegisteredEmailsResolver(GigyaLoginCallback<A> loginCallback,
                                    GigyaApiResponse interruption,
                                    IBusinessApiService<A> businessApiService,
                                    VerifyCodeResolver verifyCodeResolver) {
        super(loginCallback, interruption, businessApiService);
        _verifyCodeResolver = verifyCodeResolver;
    }

    /**
     * Request account registered emails.
     *
     * @param resultCallback Result callback.
     */
    @Override
    public void getRegisteredEmails(@NonNull final ResultCallback resultCallback) {
        GigyaLogger.debug(LOG_TAG, "getRegisteredEmails: ");

        // Initialize the TFA flow.
        final Map<String, Object> params = new HashMap<>();
        params.put("regToken", getRegToken());
        params.put("provider", GigyaDefinitions.TFAProvider.EMAIL);
        params.put("mode", "verify");
        _businessApiService.send(GigyaDefinitions.API.API_TFA_INIT, params, RestAdapter.POST,
                InitTFAModel.class, new GigyaCallback<InitTFAModel>() {

                    @Override
                    public void onSuccess(InitTFAModel model) {
                        _gigyaAssertion = model.getGigyaAssertion();
                        if (_gigyaAssertion == null) {
                            resultCallback.onError(GigyaError.unauthorizedUser());
                            return;
                        }
                        fetchEmails(resultCallback);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        resultCallback.onError(error);
                    }
                });
    }

    /*
    Fetch emails request.
     */
    private void fetchEmails(@NonNull final ResultCallback resultCallback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", _gigyaAssertion);
        _businessApiService.send(GigyaDefinitions.API.API_TFA_EMAIL_GET_EMAILS, params, RestAdapter.GET,
                GetEmailsModel.class, new GigyaCallback<GetEmailsModel>() {
                    @Override
                    public void onSuccess(GetEmailsModel model) {
                        final List<EmailModel> emails = model.getEmails();
                        resultCallback.onRegisteredEmails(emails);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        resultCallback.onError(error);
                    }
                });
    }

    /**
     * @param verifiedEmail  Verified email address.
     * @param resultCallback Result callback.
     */
    @Override
    public void sendEmailCode(@NonNull EmailModel verifiedEmail, @NonNull final ResultCallback resultCallback) {
        sendEmailCode(verifiedEmail, "en", resultCallback);
    }

    /**
     * @param verifiedEmail  Verified email address.
     * @param lang           Localization language.
     * @param resultCallback Result callback.
     */
    @Override
    public void sendEmailCode(@NonNull EmailModel verifiedEmail, @NonNull final String lang, @NonNull final ResultCallback resultCallback) {
        GigyaLogger.debug(LOG_TAG, "sendEmailCode for verified email: " + verifiedEmail.getObfuscated());

        // Send verification code.
        final Map<String, Object> params = new HashMap<>();
        params.put("emailID", verifiedEmail.getId());
        params.put("gigyaAssertion", _gigyaAssertion);
        params.put("lang", lang);
        _businessApiService.send(GigyaDefinitions.API.API_TFA_EMAIL_SEND_VERIFICATION_CODE, params, RestAdapter.POST,
                VerificationCodeModel.class, new GigyaCallback<VerificationCodeModel>() {
                    @Override
                    public void onSuccess(VerificationCodeModel model) {
                        final String phvToken = model.getPhvToken();
                        resultCallback.onEmailVerificationCodeSent(_verifyCodeResolver.withAssertionAndPhvToken(_gigyaAssertion, phvToken));
                    }

                    @Override
                    public void onError(GigyaError error) {
                        resultCallback.onError(error);
                    }
                });
    }

    public interface ResultCallback {

        void onRegisteredEmails(List<EmailModel> registeredEmailList);

        void onEmailVerificationCodeSent(IVerifyCodeResolver verifyCodeResolver);

        void onError(GigyaError error);
    }
}
