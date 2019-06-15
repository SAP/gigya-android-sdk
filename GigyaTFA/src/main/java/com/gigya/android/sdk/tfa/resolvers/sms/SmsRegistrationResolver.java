package com.gigya.android.sdk.tfa.resolvers.sms;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.interruption.Resolver;

public class SmsRegistrationResolver<A extends GigyaAccount> extends Resolver<A> implements ISmsRegistrationResolver {

    public SmsRegistrationResolver(GigyaLoginCallback<A> initiatorContext) {
        super(initiatorContext);
    }

    public interface FlowCallback {

        void onVerificationCodeSent();
    }

}
