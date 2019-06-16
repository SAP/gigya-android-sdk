package com.gigya.android.sdk.interruption.tfa;

import android.support.annotation.Nullable;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.containers.IoCContainer;

public class TfaResolverFactory {

    final private IoCContainer _container;
    final private GigyaLoginCallback _loginCallback;
    final private GigyaApiResponse _interruption;

    public TfaResolverFactory(IoCContainer container, GigyaLoginCallback loginCallback, GigyaApiResponse interruption) {
        _container = container;
        _loginCallback = loginCallback;
        _interruption = interruption;
    }

    @Nullable
    public <T> T getResolverFor(Class<T> clazz) {
        final IoCContainer resolverContainer = _container.clone();
        resolverContainer
                .bind(GigyaLoginCallback.class, _loginCallback)
                .bind(GigyaApiResponse.class, _interruption);

        try {
            return resolverContainer.createInstance(clazz);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        } finally {
            resolverContainer.dispose();
        }
    }
}
