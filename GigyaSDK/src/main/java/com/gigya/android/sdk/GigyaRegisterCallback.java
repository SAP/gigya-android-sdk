package com.gigya.android.sdk;

import com.gigya.android.sdk.network.api.ApiResolver;

public abstract class GigyaRegisterCallback<T> extends GigyaCallback<T> {

    public void onPendingRegistration(String regToken, ApiResolver<T> resolver) {
        // Stub.
    }
}
