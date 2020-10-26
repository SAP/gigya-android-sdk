package com.gigya.android.sdk.tfa.resolvers.totp;

import androidx.annotation.NonNull;

public interface IVerifyTOTPResolver {

    void verifyTOTPCode(@NonNull String verificationCode, boolean rememberDevice, @NonNull VerifyTOTPResolver.ResultCallback resultCallback);
}
