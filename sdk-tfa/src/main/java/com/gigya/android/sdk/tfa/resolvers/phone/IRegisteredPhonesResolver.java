package com.gigya.android.sdk.tfa.resolvers.phone;

import androidx.annotation.NonNull;

import com.gigya.android.sdk.tfa.GigyaDefinitions;

public interface IRegisteredPhonesResolver {

    void getPhoneNumbers(@NonNull RegisteredPhonesResolver.ResultCallback resultCallback);

    // Default language is "en"
    void sendVerificationCode(@NonNull String phoneId,
                              @NonNull @GigyaDefinitions.PhoneMethod.Method String method,
                              @NonNull RegisteredPhonesResolver.ResultCallback resultCallback);

    void sendVerificationCode(@NonNull String phoneId,
                              @NonNull String lang,
                              @NonNull @GigyaDefinitions.PhoneMethod.Method String method,
                              @NonNull RegisteredPhonesResolver.ResultCallback resultCallback);
}
