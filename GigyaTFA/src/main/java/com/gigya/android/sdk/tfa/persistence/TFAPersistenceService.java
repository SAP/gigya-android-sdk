package com.gigya.android.sdk.tfa.persistence;

import android.content.Context;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.persistence.PersistenceService;

public class TFAPersistenceService extends PersistenceService implements ITFAPersistenceService {

    public TFAPersistenceService(Context context) {
        super(context);
    }

    @Override
    public void updateOptInState(boolean enabled) {
        getPrefs().edit().putBoolean(PREFS_PUSH_TFA_OPT_IN, enabled).apply();
    }

    @Override
    public boolean isOptInForPushTFA() {
        return getPrefs().getBoolean(PREFS_PUSH_TFA_OPT_IN, false);
    }

    @Override
    public String getSessionEncryptionType() {
        return super.getSessionEncryptionType();
    }

    //region KEYS

    /*
     * Push opt-in state.
     */
    private static final String PREFS_PUSH_TFA_OPT_IN = "GS_PUSH_TFA_OPT_IN";

    //endregion
}
