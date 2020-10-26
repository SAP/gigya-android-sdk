package com.gigya.android.sdk.tfa.resolvers.totp;

import androidx.annotation.NonNull;

public interface IRegisterTOTPResolver {

    void registerTOTP(@NonNull RegisterTOTPResolver.ResultCallback resultCallback);

}
