package com.gigya.android.sdk.providers;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.api.IApiObservable;
import com.gigya.android.sdk.providers.provider.Provider;

public interface IProviderFactory {

    Provider providerFor(String name, IApiObservable observable, GigyaLoginCallback gigyaLoginCallback);
    void logoutFromUsedSocialProviders();
}
