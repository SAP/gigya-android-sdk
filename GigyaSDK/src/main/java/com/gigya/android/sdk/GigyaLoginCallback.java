package com.gigya.android.sdk;

public abstract class GigyaLoginCallback<T> extends GigyaCallback<T> {

    public abstract void onCancelledOperation();

    public void onIntermediateLoad() {
        // Stub.
    }
}
