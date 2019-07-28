package com.gigya.android.sdk.tfa.persistence;

import android.support.annotation.Nullable;

public interface ITFAPersistenceService {

    void setPushToken(String pushToken);

    @Nullable
    String getPushToken();

    void updateOptInState(boolean enabled);

    boolean isOptInForPushTFA();

    String getSessionEncryptionType();
}
