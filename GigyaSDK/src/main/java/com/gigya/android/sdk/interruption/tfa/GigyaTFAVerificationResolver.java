package com.gigya.android.sdk.interruption.tfa;

import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.api.IApiService;
import com.gigya.android.sdk.interruption.GigyaTFAResolver;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.model.tfa.TFAEmail;
import com.gigya.android.sdk.model.tfa.TFARegisteredPhone;
import com.gigya.android.sdk.network.GigyaApiResponse;

import static com.gigya.android.sdk.GigyaDefinitions.TFA.EMAIL;
import static com.gigya.android.sdk.GigyaDefinitions.TFA.PHONE;
import static com.gigya.android.sdk.GigyaDefinitions.TFA.TOTP;

public class GigyaTFAVerificationResolver<A extends GigyaAccount> extends GigyaTFAResolver<A> implements IGigyaTFAVerificationResolver {

    public GigyaTFAVerificationResolver(IApiService apiService, GigyaApiResponse originalResponse, GigyaLoginCallback<A> loginCallback) {
        super(apiService, originalResponse, loginCallback);
    }

    @Override
    public void startVerifyWithEmail() {
        verifyEmail();
    }

    @Override
    public void startVerifyWithPhone() {
        verifyPhone();
    }

    @Override
    public void startVerifyWithTotp(String authCode) {
        verifyTotp(authCode);
    }

    @Override
    public void sendCodeToPhone(TFARegisteredPhone registeredPhone) {
        sendPhoneVerificationCode(registeredPhone.getId(), registeredPhone.getLastMethod(), true);
    }

    @Override
    public void sendCodeToeEmail(TFAEmail tfaEmail) {
        verifyWithEmail(tfaEmail);
    }

    @Override
    public void verifyCode(@GigyaDefinitions.TFA.TFAProvider String provider, String authCode) {
        switch (provider) {
            case TOTP:
                submitTotpCode(authCode);
                break;
            case PHONE:
                submitPhoneCode(authCode);
                break;
            case EMAIL:
                submitEmailCode(authCode);
                break;
        }
    }


    @Override
    protected void forwardInitialInterruption() {
        if (isAttached()) {
            _loginCallback.get().onPendingTFAVerification(_originalResponse, this);
        }
    }
}
