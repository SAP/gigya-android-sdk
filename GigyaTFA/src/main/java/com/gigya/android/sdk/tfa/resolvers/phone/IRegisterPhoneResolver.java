package com.gigya.android.sdk.tfa.resolvers.phone;

import android.support.annotation.NonNull;

import com.gigya.android.sdk.tfa.GigyaDefinitions;

public interface IRegisterPhoneResolver {

    // Default method is "sms".
    void registerPhone(@NonNull String phoneNumber,
                       @NonNull RegisterPhoneResolver.ResultCallback resultCallback);

    void registerPhone(@NonNull String phoneNumber,
                       @NonNull @GigyaDefinitions.PhoneMethod.Method String method,
                       @NonNull RegisterPhoneResolver.ResultCallback resultCallback);

}

