package com.gigya.android.sdk.tfa.resolvers.sms;

import android.support.annotation.NonNull;

public interface IPhoneVerificationResolver<A> {

    void verifyCode(@NonNull final String regToken,
                    @NonNull String verificationCode,
                    @NonNull String gigyaAssertion,
                    @NonNull String phvToken,
                    @NonNull PhoneVerificationResolver.VerificationCallback<A> verificationCallback);
}
