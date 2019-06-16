package com.gigya.android.sdk.interruption;

import com.gigya.android.sdk.api.IBusinessApiService;

public class Resolver<A> {

    final protected IBusinessApiService<A> _businessApiService;

    public Resolver(IBusinessApiService<A> businessApiService) {
        _businessApiService = businessApiService;
    }
}
