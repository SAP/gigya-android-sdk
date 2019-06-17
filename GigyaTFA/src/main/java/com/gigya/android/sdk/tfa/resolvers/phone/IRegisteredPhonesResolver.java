package com.gigya.android.sdk.tfa.resolvers.phone;

import android.support.annotation.NonNull;

import com.gigya.android.sdk.tfa.GigyaDefinitions;

public interface IRegisteredPhonesResolver {

    void getPhoneNumbers(@NonNull RegisteredPhonesResolver.ResultCallback resultCallback);

    void sendVerificationCode(@NonNull String phoneId, @NonNull @GigyaDefinitions.PhoneMethod.Method String method, @NonNull RegisteredPhonesResolver.ResultCallback resultCallback);
}
