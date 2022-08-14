package com.gigya.android.sdk.session;


import com.gigya.android.sdk.GigyaLogger;

import java.util.ArrayList;

public class SessionStateHandler {

    private static final String TAG = "SessionStateHandler";

    private final ArrayList<SessionStateObserver> mExpirationStateObservers = new ArrayList<>();
    private final ArrayList<SessionStateObserver> mVerificationStateObservers = new ArrayList<>();

    public void registerExpirationObserver(SessionStateObserver observer) {
        GigyaLogger.debug(TAG, "registerExpirationObserver: " + System.identityHashCode(observer));
        mExpirationStateObservers.add(observer);
    }

    public void registerVerificationObserver(SessionStateObserver observer) {
        GigyaLogger.debug(TAG, "registerVerificationObserver: " + System.identityHashCode(observer));

        mVerificationStateObservers.add(observer);
    }

    public void removeExpirationObserver(SessionStateObserver observer) {
        GigyaLogger.debug(TAG, "removeExpirationObserver: " + System.identityHashCode(observer));
        mExpirationStateObservers.remove(observer);
    }

    public void removeVerificationObserver(SessionStateObserver observer) {
        GigyaLogger.debug(TAG, "removeVerificationObserver: " + System.identityHashCode(observer));
        mVerificationStateObservers.remove(observer);
    }

    public void notifySessionExpired() {
        GigyaLogger.debug(TAG, "notifySessionExpired");
        for (SessionStateObserver observer : mExpirationStateObservers) {
            GigyaLogger.debug(TAG, "notifySessionExpired for : " + System.identityHashCode(observer));
            observer.onSessionInvalidated(null);
        }
    }

    public void notifySessionInvalidated(Object data) {
        GigyaLogger.debug(TAG, "notifySessionInvalidated");
        for (SessionStateObserver observer : mVerificationStateObservers) {
            GigyaLogger.debug(TAG, "notifySessionInvalidated for : " + System.identityHashCode(observer));
            observer.onSessionInvalidated(data);
        }
    }
}

