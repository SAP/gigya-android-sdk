package com.gigya.android.sdk.auth.resolvers;

import androidx.annotation.NonNull;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.auth.GigyaDefinitions;
import com.gigya.android.sdk.interruption.Resolver;
import com.gigya.android.sdk.interruption.tfa.TFAResolver;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.RestAdapter;

import java.util.HashMap;
import java.util.Map;

public class OTPRegistrationResolver<A extends GigyaAccount> extends Resolver<A> implements IGigyaOtpResult {

    private static final String LOG_TAG = "OTPRegistrationResolver";

    boolean _updateState;

    private final String _vToken;
    private Map<String, Object> _sendParams;

    public OTPRegistrationResolver(GigyaLoginCallback<A> loginCallback,
                                   GigyaApiResponse interruption,
                                   IBusinessApiService<A> businessApiService,
                                   Map<String, Object> sendParams,
                                   boolean updateState) {
        super(loginCallback, interruption, businessApiService);
        _updateState = updateState;
        _sendParams = sendParams;
        _vToken = interruption.getField("vToken", String.class);
    }

    @Override
    public void verify(@NonNull String code) {
        String api = GigyaDefinitions.API.API_AUTH_OTP_LOGIN;
        if (_updateState) {
            api = GigyaDefinitions.API.API_AUTH_OTP_UPDATE;
        }
        if (_sendParams == null) {
            _sendParams = new HashMap<>();
        }
        _sendParams.put("vToken", _vToken);
        _sendParams.put("code", code);

        // Removing unwanted parameters from original call.
        _sendParams.remove("phoneNumber");
        _sendParams.remove("lang");

        _businessApiService.send(api, _sendParams, RestAdapter.POST,
                GigyaApiResponse.class, new GigyaCallback<GigyaApiResponse>() {

                    @Override
                    public void onSuccess(GigyaApiResponse model) {
                        GigyaLogger.debug(LOG_TAG, "otpLogin: successfully verified push authentication request");
                        if (_updateState) {
                            _businessApiService.getAccountService().invalidateAccount();
                            _businessApiService.getAccount(_sendParams, _loginCallback);
                            return;
                        }
                        _businessApiService.handleAccountApiResponse(model, _loginCallback);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        GigyaLogger.error(LOG_TAG, "otpLogin: failed to verify push authentication request with error " + error.getErrorCode());
                        if (_updateState) {
                            _loginCallback.onError(error);
                            return;
                        }
                        _businessApiService.handleAccountApiResponse(
                                new GigyaApiResponse(error.getData()),
                                _loginCallback);
                    }
                });
    }
}
