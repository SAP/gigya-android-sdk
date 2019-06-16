package com.gigya.android.sdk.interruption;

import android.support.annotation.NonNull;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.network.GigyaError;

import java.util.Map;

public class PendingRegistrationResolver<A extends GigyaAccount> extends Resolver<A> implements IPendingRegistrationResolver<A> {

    public PendingRegistrationResolver(GigyaLoginCallback<A> loginCallback,
                                       GigyaApiResponse interruption,
                                       IBusinessApiService<A> businessApiService) {
        super(loginCallback, interruption, businessApiService);
    }

    @Override
    public void setAccount(@NonNull Map<String, Object> params) {
        _businessApiService.setAccount(params, new GigyaCallback<A>() {
            @Override
            public void onSuccess(A updatedAccount) {
                finalizeRegistration();
            }

            @Override
            public void onError(GigyaError error) {
                _loginCallback.onError(error);
            }
        });
    }
}
