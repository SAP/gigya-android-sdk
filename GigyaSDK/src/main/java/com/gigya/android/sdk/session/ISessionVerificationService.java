package com.gigya.android.sdk.session;

public interface ISessionVerificationService {

    void registerActivityLifecycleCallbacks();

    void start();

    void stop();

    long getInitialDelay();
}
