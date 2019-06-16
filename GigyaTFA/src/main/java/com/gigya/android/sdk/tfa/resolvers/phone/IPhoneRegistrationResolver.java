package com.gigya.android.sdk.tfa.resolvers.phone;

import android.support.annotation.NonNull;

public interface IPhoneRegistrationResolver {

    // Default method is "sms".
    void register(@NonNull String regToken,
                  @NonNull String phoneNumber,
                  @NonNull PhoneRegistrationResolver.RegistrationCallback registrationCallback);

    void register(@NonNull String regToken,
                  @NonNull String phoneNumber,
                  @NonNull String method,
                  @NonNull PhoneRegistrationResolver.RegistrationCallback registrationCallback);

}

