package com.gigya.android.sdk.tfa.resolvers;

import android.support.annotation.NonNull;

import com.gigya.android.sdk.tfa.GigyaDefinitions;

public interface IVerifyCodeResolver {

    void verifyCode(@NonNull @GigyaDefinitions.TFAProvider.Provider String provider, @NonNull String verificationCode, @NonNull VerifyCodeResolver.ResultCallback resultCallback);
}
