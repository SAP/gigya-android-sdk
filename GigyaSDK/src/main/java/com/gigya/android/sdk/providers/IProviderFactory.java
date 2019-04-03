package com.gigya.android.sdk.providers;

import com.gigya.android.sdk.GigyaLoginCallback;

public interface IProviderFactory {

    Provider providerFor(String name, GigyaLoginCallback gigyaLoginCallback);
}
