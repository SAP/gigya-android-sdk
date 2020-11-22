package com.gigya.android.sdk.tfa.resolvers;

import androidx.annotation.NonNull;

import com.gigya.android.sdk.tfa.GigyaDefinitions;

public interface IVerifyCodeResolver {

    void verifyCode(@NonNull @GigyaDefinitions.TFAProvider.Provider String provider,
                    @NonNull String verificationCode,
                    boolean rememberDevice,
                    @NonNull VerifyCodeResolver.ResultCallback resultCallback);
}
