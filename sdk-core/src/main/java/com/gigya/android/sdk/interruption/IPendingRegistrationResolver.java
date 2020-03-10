package com.gigya.android.sdk.interruption;

import android.support.annotation.NonNull;

import java.util.Map;

public interface IPendingRegistrationResolver {

    void setAccount(@NonNull final Map<String, Object> params);
}
