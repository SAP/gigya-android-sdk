package com.gigya.android.sdk;

import android.support.annotation.NonNull;

public abstract class GigyaLoginCallback<T> extends GigyaCallback<T> {

    public void onCancelledOperation() {
        // Stub.
    }

    public void onIntermediateLoad() {
        // Stub.
    }

    public void onPendingVerification(@NonNull String regToken) {
        // Stub.
    }

    public void onPendingRegistration(@NonNull String regToken) {
        // Stub.
    }

    public void onPendingPasswordChange() {
        // Stub.
    }
}
