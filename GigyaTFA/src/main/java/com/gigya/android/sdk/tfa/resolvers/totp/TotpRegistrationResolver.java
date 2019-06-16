package com.gigya.android.sdk.tfa.resolvers.totp;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.interruption.Resolver;

public class TotpRegistrationResolver<A extends GigyaAccount> extends Resolver<A> implements ITotpRegistrationResolver {

    public TotpRegistrationResolver(IBusinessApiService<A> businessApiService) {
        super(businessApiService);
    }
}
