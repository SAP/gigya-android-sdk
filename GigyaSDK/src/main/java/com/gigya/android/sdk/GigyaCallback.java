package com.gigya.android.sdk;

import com.gigya.android.sdk.network.GigyaError;

// TODO: 17/01/2019 Code review 14/1/19 -> Refactor to multiple purpose callbacks.

public abstract class GigyaCallback<T> {

    public abstract void onSuccess(T obj);

    public abstract void onError(GigyaError error);

    public void onOperationCancelled() {
        // Stub.
    }

    public void onIntermediateLoad() {
        // Stub.
    }
}

