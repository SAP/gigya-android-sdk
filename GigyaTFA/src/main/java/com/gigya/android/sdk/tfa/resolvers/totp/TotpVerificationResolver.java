package com.gigya.android.sdk.tfa.resolvers.totp;

import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.interruption.Resolver;

public class TotpVerificationResolver<A extends GigyaAccount> extends Resolver<A> implements ITotpVerificationResolver {

    public TotpVerificationResolver(IBusinessApiService<A> businessApiService) {
        super(businessApiService);
    }
}
