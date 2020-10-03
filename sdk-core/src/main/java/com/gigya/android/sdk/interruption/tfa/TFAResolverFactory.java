package com.gigya.android.sdk.interruption.tfa;

import androidx.annotation.Nullable;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.containers.IoCContainer;

public class TFAResolverFactory {

    final private IoCContainer _container;
    final private GigyaLoginCallback _loginCallback;
    final private GigyaApiResponse _interruption;

    public TFAResolverFactory(IoCContainer container, GigyaLoginCallback loginCallback, GigyaApiResponse interruption) {
        _container = container;
        _loginCallback = loginCallback;
        _interruption = interruption;
    }

    @Nullable
    public <T extends TFAResolver> T getResolverFor(Class<T> clazz) {

        final IoCContainer resolverContainer = _container.clone();
        resolverContainer
                .bind(GigyaLoginCallback.class, _loginCallback)
                .bind(GigyaApiResponse.class, _interruption);

        try {
            return resolverContainer.createInstance(clazz, true);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        } finally {
            resolverContainer.dispose();
        }
    }
}
