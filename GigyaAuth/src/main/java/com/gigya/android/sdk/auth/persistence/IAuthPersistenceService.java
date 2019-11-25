package com.gigya.android.sdk.auth.persistence;

import com.gigya.android.sdk.persistence.IPersistenceService;

public interface IAuthPersistenceService extends IPersistenceService {

    void updateAuthPushState(boolean enabled);

    boolean isRegisteredForAuthPush();
}
