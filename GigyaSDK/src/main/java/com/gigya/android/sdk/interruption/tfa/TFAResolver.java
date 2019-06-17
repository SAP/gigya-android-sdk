package com.gigya.android.sdk.interruption.tfa;

import android.support.annotation.Nullable;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.interruption.Resolver;

public class TFAResolver<A extends GigyaAccount> extends Resolver<A> {

    @Nullable
    protected String _gigyaAssertion;

    public TFAResolver(GigyaLoginCallback<A> loginCallback,
                       GigyaApiResponse interruption,
                       IBusinessApiService<A> businessApiService) {
        super(loginCallback, interruption, businessApiService);
    }
}
