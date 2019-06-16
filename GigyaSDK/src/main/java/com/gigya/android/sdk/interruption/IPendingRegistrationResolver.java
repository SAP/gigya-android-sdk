package com.gigya.android.sdk.interruption;

import android.support.annotation.NonNull;

import com.gigya.android.sdk.account.models.GigyaAccount;

import java.util.Map;

public interface IPendingRegistrationResolver<A extends GigyaAccount> {

    void setAccount(@NonNull final Map<String, Object> params);
}
