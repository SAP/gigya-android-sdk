package com.gigya.android.sdk.interruption;

import com.gigya.android.sdk.GigyaLoginCallback;

import java.lang.ref.SoftReference;

public class Resolver<A> {

    final SoftReference<GigyaLoginCallback<A>> initiatorContext;

    public Resolver(final GigyaLoginCallback<A> initiatorContext) {
        this.initiatorContext = new SoftReference<>(initiatorContext);
    }
}
