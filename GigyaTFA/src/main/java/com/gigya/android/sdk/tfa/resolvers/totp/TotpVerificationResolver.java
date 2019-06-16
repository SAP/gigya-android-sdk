package com.gigya.android.sdk.tfa.resolvers.totp;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.interruption.Resolver;

public class TotpVerificationResolver<A extends GigyaAccount> extends Resolver<A> implements ITotpVerificationResolver {

    public TotpVerificationResolver(GigyaLoginCallback<A> loginCallback,
                                    GigyaApiResponse interruption,
                                    IBusinessApiService<A> businessApiService) {
        super(loginCallback, interruption, businessApiService);
    }
}
