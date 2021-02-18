package com.gigya.android.sdk.auth;

import androidx.annotation.NonNull;

import com.gigya.android.sdk.account.models.GigyaAccount;

import java.util.Map;

public interface IOTP {

    <A extends GigyaAccount> void phoneLogin(@NonNull String phoneNumber, @NonNull final GigyaOTPCallback<A> gigyaCallback);

    <A extends GigyaAccount> void phoneLogin(@NonNull String phoneNumber, @NonNull Map<String, Object> params, @NonNull final GigyaOTPCallback<A> gigyaCallback);

    <A extends GigyaAccount> void phoneUpdate(@NonNull String phoneNumber, @NonNull final GigyaOTPCallback<A> gigyaCallback);

    <A extends GigyaAccount> void phoneUpdate(@NonNull String phoneNumber, @NonNull Map<String, Object> params, @NonNull final GigyaOTPCallback<A> gigyaCallback);

}
