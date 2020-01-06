package com.gigya.android.sdk;

import com.gigya.android.sdk.network.GigyaError;

/**
 * Basic gigya API response callback.
 *
 * @param <T> Optional dynamic type for response object. If none supplied will use GigyaApiResponse as default.
 */
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

