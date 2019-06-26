package com.gigya.android.sdk.tfa.resolvers.totp;

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
import com.gigya.android.sdk.tfa.models.TOTPRegisterModel;

import java.util.HashMap;
import java.util.Map;

public class RegisterTOTPResolver<A extends GigyaAccount> extends TFAResolver<A> implements IRegisterTOTPResolver {

    private static final String LOG_TAG = "RegisterTOTPResolver";

    private final VerifyTOTPResolver<A> _verifyTOTPresolver;

    public RegisterTOTPResolver(GigyaLoginCallback<A> loginCallback,
                                GigyaApiResponse interruption,
                                IBusinessApiService<A> businessApiService,
                                VerifyTOTPResolver<A> verifyTOTPResolver) {
        super(loginCallback, interruption, businessApiService);
        _verifyTOTPresolver = verifyTOTPResolver;
    }

    @Override
    public void registerTOTP(@NonNull final ResultCallback resultCallback) {
        GigyaLogger.debug(LOG_TAG, "registerTOTP: ");

        // Initialize the TFA flow.
        final Map<String, Object> params = new HashMap<>();
        params.put("regToken", getRegToken());
        params.put("provider", GigyaDefinitions.TFAProvider.TOTP);
        params.put("mode", "register");
        _businessApiService.send(GigyaDefinitions.API.API_TFA_INIT, params, RestAdapter.POST,
                InitTFAModel.class, new GigyaCallback<InitTFAModel>() {
                    @Override
                    public void onSuccess(InitTFAModel model) {
                        _gigyaAssertion = model.getGigyaAssertion();
                        if (_gigyaAssertion == null) {
                            resultCallback.onError(GigyaError.unauthorizedUser());
                            return;
                        }
                        getQRCode(resultCallback);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        resultCallback.onError(error);
                    }
                });
    }

    private void getQRCode(@NonNull final ResultCallback resultCallback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", _gigyaAssertion);
        _businessApiService.send(GigyaDefinitions.API.API_TFA_TOTP_REGISTER, params, RestAdapter.POST,
                TOTPRegisterModel.class, new GigyaCallback<TOTPRegisterModel>() {
                    @Override
                    public void onSuccess(TOTPRegisterModel model) {
                        final String sctToken = model.getSctToken();
                        final String qrCode = model.getQrCode();
                        resultCallback.onQRCodeAvailable(qrCode, _verifyTOTPresolver.withAssertionAndSctToken(_gigyaAssertion, sctToken));
                    }

                    @Override
                    public void onError(GigyaError error) {
                        resultCallback.onError(error);
                    }
                });
    }

    public interface ResultCallback {

        void onQRCodeAvailable(@NonNull String qrCode, IVerifyTOTPResolver verifyTOTPResolver);

        void onError(GigyaError error);
    }
}
