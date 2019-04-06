package com.gigya.android.sdk.interruption;

public interface IGigyaResolver {

    void init();

    void clear();

    boolean isAttached();
}
