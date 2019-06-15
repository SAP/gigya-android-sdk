package com.gigya.android.sdk.tfa.resolvers.sms;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.interruption.Resolver;

public class SmsVerificationResolver<A extends GigyaAccount> extends Resolver<A> implements ISmsVerificationResolver {

    public SmsVerificationResolver(GigyaLoginCallback<A> initiatorContext) {
        super(initiatorContext);
    }
}
