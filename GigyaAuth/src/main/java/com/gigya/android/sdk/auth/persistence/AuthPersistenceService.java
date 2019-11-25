package com.gigya.android.sdk.auth.persistence;

import android.content.Context;

import com.gigya.android.sdk.persistence.PersistenceService;

public class AuthPersistenceService extends PersistenceService implements IAuthPersistenceService {

    public AuthPersistenceService(Context context) {
        super(context);
    }

    @Override
    public String getSessionEncryptionType() {
        return super.getSessionEncryptionType();
    }

    //region KEYS

    /*
     * Push opt-in state.
     */
    private static final String PREFS_PUSH_AUTH_REGISTERED = "GS_PUSH_AUTH_REGISTERED";

    @Override
    public void updateAuthPushState(boolean enabled) {
        getPrefs().edit().putBoolean(PREFS_PUSH_AUTH_REGISTERED, enabled).apply();

    }

    @Override
    public boolean isRegisteredForAuthPush() {
        return getPrefs().getBoolean(PREFS_PUSH_AUTH_REGISTERED, false);
    }

    //endregion
}
