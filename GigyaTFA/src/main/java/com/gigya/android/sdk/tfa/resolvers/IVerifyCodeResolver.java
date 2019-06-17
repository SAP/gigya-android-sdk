package com.gigya.android.sdk.tfa.resolvers;

import android.support.annotation.NonNull;

public interface IVerifyCodeResolver {

    void verifyCode(@NonNull String verificationCode, @NonNull VerifyCodeResolver.ResultCallback resultCallback);
}
