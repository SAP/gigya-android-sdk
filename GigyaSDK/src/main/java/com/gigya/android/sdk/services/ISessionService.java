package com.gigya.android.sdk.services;

import com.gigya.android.sdk.model.GigyaInterceptor;
import com.gigya.android.sdk.model.account.SessionInfo;

import java.security.Key;

public interface ISessionService {

    String encryptSession(String plan, Key key);

    String decryptSession(String encrypted, Key key);

    void save(SessionInfo sessionInfo);

    void load();

    SessionInfo getSession();

    void setSession(SessionInfo sessionInfo);

    boolean isValid();

    void clear(boolean clearStorage);

    void startSessionCountdownTimerIfNeeded();

    void cancelSessionCountdownTimer();

    void addInterceptor(GigyaInterceptor interceptor);

    void refreshSessionExpiration();
}
