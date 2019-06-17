package com.gigya.android.sdk.tfa.resolvers.phone;

import android.support.annotation.NonNull;

public interface IVerifyCodeResolver {

    void verifyCode(@NonNull String verificationCode, @NonNull VerifyCodeResolver.ResultCallback resultCallback);
}
