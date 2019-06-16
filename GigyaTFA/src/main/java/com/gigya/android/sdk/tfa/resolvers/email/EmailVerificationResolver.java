package com.gigya.android.sdk.tfa.resolvers.email;

import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.interruption.Resolver;

public class EmailVerificationResolver<A extends GigyaAccount> extends Resolver<A> implements IEmailVerificationResolver {

    public EmailVerificationResolver(IBusinessApiService<A> businessApiService) {
        super(businessApiService);
    }
}
