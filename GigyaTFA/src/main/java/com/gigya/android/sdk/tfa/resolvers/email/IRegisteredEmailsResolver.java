package com.gigya.android.sdk.tfa.resolvers.email;

import android.support.annotation.NonNull;

import com.gigya.android.sdk.tfa.models.EmailModel;

public interface IRegisteredEmailsResolver {

    void getRegisteredEmails(@NonNull RegisteredEmailsResolver.ResultCallback resultCallback);

    void sendEmailCode(@NonNull EmailModel verifiedEmail, @NonNull RegisteredEmailsResolver.ResultCallback resultCallback);
}
