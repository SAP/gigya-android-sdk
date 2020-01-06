package com.gigya.android.sdk.providers;

public interface IProviderTokenTrackerListener {

    void onTokenChange(String provider, String providerSession, IProviderPermissionsCallback permissionsCallback);
}
