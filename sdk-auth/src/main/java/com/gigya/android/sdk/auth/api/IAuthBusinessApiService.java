package com.gigya.android.sdk.auth.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.auth.GigyaOTPCallback;

import java.util.Map;

public interface IAuthBusinessApiService {

    void registerDevice(@NonNull final String deviceInfo, @NonNull final GigyaCallback<GigyaApiResponse> gigyaCallback);

    void unregisterDevice(@NonNull final GigyaCallback<GigyaApiResponse> gigyaCallback);

    void verifyPush(@NonNull final String vToken, @NonNull final GigyaCallback<GigyaApiResponse> gigyaCallback);

    <A extends GigyaAccount> void otpPhoneLogin(@Nullable String phoneNumber, @NonNull Map<String, Object> params, @NonNull final GigyaOTPCallback<A> gigyaCallback);

    <A extends GigyaAccount> void otpPhoneUpdate(@Nullable String phoneNumber, @NonNull Map<String, Object> params, @NonNull final GigyaOTPCallback<A> gigyaCallback);

}
