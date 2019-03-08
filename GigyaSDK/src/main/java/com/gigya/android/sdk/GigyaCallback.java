package com.gigya.android.sdk;

import com.gigya.android.sdk.network.GigyaError;

public abstract class GigyaCallback<T> {

    public abstract void onSuccess(T obj);

    public abstract void onError(GigyaError error);

    public void onOperationCanceled() {
        // Stub.
    }

    public void onIntermediateLoad() {
        // Stub.
    }
}

