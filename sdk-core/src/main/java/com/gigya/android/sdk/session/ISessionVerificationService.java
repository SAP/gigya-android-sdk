package com.gigya.android.sdk.session;

public interface ISessionVerificationService {

    void updateInterval();

    void registerActivityLifecycleCallbacks();

    void start();

    void stop();

    long getInitialDelay();

    void addObserver(SessionVerificationObserver observer);

    void removeObserver(SessionVerificationObserver observer);

}
