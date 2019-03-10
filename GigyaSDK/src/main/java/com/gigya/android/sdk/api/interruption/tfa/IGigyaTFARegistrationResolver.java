package com.gigya.android.sdk.api.interruption.tfa;

import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.model.tfa.TFAProvider;

import java.util.List;

public interface IGigyaTFARegistrationResolver {

    List<TFAProvider> getInactiveProviders();

    void startRegistrationWithPhone(String phoneNumber, String method);

    void startRegistrationWithPhone(String phoneNumber);

    void startRegistrationWithTotp();

    void verifyCode(@GigyaDefinitions.TFA.TFAProvider String provider, String authCode);

    void clear();
}
