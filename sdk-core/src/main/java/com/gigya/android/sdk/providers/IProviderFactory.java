package com.gigya.android.sdk.providers;

import androidx.annotation.Nullable;

import com.gigya.android.sdk.providers.provider.Provider;
import com.gigya.android.sdk.providers.provider.ProviderCallback;

public interface IProviderFactory {

    Provider providerFor(String name, ProviderCallback providerCallback);

    @Nullable
    Provider usedProviderFor(String name);

    void logoutFromUsedSocialProviders();
}
