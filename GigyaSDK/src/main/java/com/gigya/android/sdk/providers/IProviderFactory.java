package com.gigya.android.sdk.providers;

import android.support.annotation.Nullable;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.providers.provider.Provider;
import com.gigya.android.sdk.providers.provider.ProviderCallback;

public interface IProviderFactory {

    Provider providerFor(String name, ProviderCallback providerCallback);

    @Nullable
    Provider usedProviderFor(String name);

    void logoutFromUsedSocialProviders();
}
