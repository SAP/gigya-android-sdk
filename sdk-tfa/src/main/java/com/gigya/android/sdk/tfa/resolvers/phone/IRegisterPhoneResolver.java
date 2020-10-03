package com.gigya.android.sdk.tfa.resolvers.phone;

import androidx.annotation.NonNull;

import com.gigya.android.sdk.tfa.GigyaDefinitions;

public interface IRegisterPhoneResolver {

    // Default method is "sms". Default language is "en".
    void registerPhone(@NonNull String phoneNumber,
                       @NonNull RegisterPhoneResolver.ResultCallback resultCallback);


    // Default language is "en".
    void registerPhone(@NonNull String phoneNumber,
                       @NonNull @GigyaDefinitions.PhoneMethod.Method String method,
                       @NonNull RegisterPhoneResolver.ResultCallback resultCallback);

    void registerPhone(@NonNull String phoneNumber,
                       @NonNull String lang,
                       @NonNull @GigyaDefinitions.PhoneMethod.Method String method,
                       @NonNull RegisterPhoneResolver.ResultCallback resultCallback);

}

