package com.gigya.android.sdk.session;

import com.gigya.android.sdk.GigyaInterceptor;

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

    void setClearCookies(boolean clear);

    void clearCookiesOnLogout();

    void addSessionExpirationObserver(SessionExpirationObserver observer);

    void removeSessionExpirationObserver(SessionExpirationObserver observer);
}
