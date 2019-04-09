package com.gigya.android.sdk.interruption.tfa;

import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.api.IApiService;
import com.gigya.android.sdk.interruption.GigyaTFAResolver;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.network.GigyaApiResponse;

import static com.gigya.android.sdk.GigyaDefinitions.TFA.PHONE;
import static com.gigya.android.sdk.GigyaDefinitions.TFA.TOTP;

public class GigyaTFARegistrationResolver<A extends GigyaAccount> extends GigyaTFAResolver<A> implements IGigyaTFARegistrationResolver {

    public GigyaTFARegistrationResolver(IApiService apiService, GigyaApiResponse originalResponse, GigyaLoginCallback<A> loginCallback) {
        super(apiService, originalResponse, loginCallback);
    }

    @Override
    public void startRegistrationWithPhone(String phoneNumber, String method) {
        registerPhone(phoneNumber, method);
    }

    @Override
    public void startRegistrationWithPhone(String phoneNumber) {
        registerPhone(phoneNumber, "sms");
    }

    @Override
    public void startRegistrationWithTotp() {
        registerTotp();
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
        }
    }

    @Override
    protected void forwardInitialInterruption() {
        if (isAttached()) {
            _loginCallback.get().onPendingTFARegistration(_originalResponse, this);
        }
    }

}
