package com.gigya.android.sdk.session;

public interface ISessionVerificationService {

    void updateInterval();

    void registerActivityLifecycleCallbacks();

    void start();

    void stop();

    void registerObserver(SessionStateObserver observer);

    void removeObserver(SessionStateObserver observer);

}
