package com.gigya.android.sdk.interruption;

public interface IGigyaResolver {

    void start();

    void clear();

    boolean isAttached();
}
