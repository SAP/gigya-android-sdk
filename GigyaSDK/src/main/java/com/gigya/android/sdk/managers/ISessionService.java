package com.gigya.android.sdk.managers;

import com.gigya.android.sdk.model.account.SessionInfo;

import java.security.Key;

public interface ISessionService {

    String encryptSession(String plan, Key key);

    String decryptSession(String encrypted, Key key);

    void save(SessionInfo sessionInfo);

    SessionInfo load();

    SessionInfo getSession();

    void setSession(SessionInfo sessionInfo);

    boolean isValid();
}
