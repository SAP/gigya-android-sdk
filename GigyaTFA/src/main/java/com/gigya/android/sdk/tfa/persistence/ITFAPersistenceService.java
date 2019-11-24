package com.gigya.android.sdk.tfa.persistence;

import com.gigya.android.sdk.persistence.IPersistenceService;

public interface ITFAPersistenceService extends IPersistenceService {

    void updateOptInState(boolean enabled);

    boolean isOptInForPushTFA();

    String getSessionEncryptionType();
}
