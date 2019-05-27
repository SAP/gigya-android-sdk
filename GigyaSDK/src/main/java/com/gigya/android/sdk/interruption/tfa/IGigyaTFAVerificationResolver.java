package com.gigya.android.sdk.interruption.tfa;

import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.interruption.tfa.models.TFAEmail;
import com.gigya.android.sdk.interruption.tfa.models.TFAProvider;
import com.gigya.android.sdk.interruption.tfa.models.TFARegisteredPhone;

import java.util.List;

public interface IGigyaTFAVerificationResolver {

    List<TFAProvider> getActiveProviders();

    void startVerifyWithEmail();

    void startVerifyWithPhone();

    void startVerifyWithTotp(String authCode);

    void sendCodeToPhone(TFARegisteredPhone registeredPhone);

    void sendCodeToeEmail(TFAEmail tfaEmail);

    void verifyCode(@GigyaDefinitions.TFA.TFAProvider String provider, String authCode);

    void clear();
}
