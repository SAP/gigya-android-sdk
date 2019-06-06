package com.gigya.android.sdk.providers;

import android.support.annotation.Nullable;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.providers.provider.Provider;

public interface IProviderFactory {

    Provider providerFor(String name, GigyaLoginCallback gigyaLoginCallback);

    @Nullable
    Provider usedProviderFor(String name);

    void logoutFromUsedSocialProviders();
}
