package com.gigya.android.sdk.providers;

import android.content.Context;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.api.ApiObservable;
import com.gigya.android.sdk.api.IApiObservable;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.providers.provider.FacebookProvider;
import com.gigya.android.sdk.providers.provider.GoogleProvider;
import com.gigya.android.sdk.providers.provider.IProvider;
import com.gigya.android.sdk.providers.provider.LineProvider;
import com.gigya.android.sdk.providers.provider.Provider;
import com.gigya.android.sdk.providers.provider.WeChatProvider;
import com.gigya.android.sdk.providers.provider.WebLoginProvider;
import com.gigya.android.sdk.utils.FileUtils;

import java.util.HashSet;
import java.util.Set;

import static com.gigya.android.sdk.GigyaDefinitions.Providers.FACEBOOK;
import static com.gigya.android.sdk.GigyaDefinitions.Providers.GOOGLE;
import static com.gigya.android.sdk.GigyaDefinitions.Providers.LINE;
import static com.gigya.android.sdk.GigyaDefinitions.Providers.WECHAT;

public class ProviderFactory implements IProviderFactory {

    final private IoCContainer _container;
    final private Context _context;
    final private FileUtils _fileUtils;
    final private IPersistenceService _psService;

    public ProviderFactory(IoCContainer container,
                           Context context,
                           FileUtils fileUtils,
                           IPersistenceService persistenceService) {
        _container = container;
        _context = context;
        _fileUtils = fileUtils;
        _psService = persistenceService;
    }

    @Override
    public Provider providerFor(String name, IApiObservable observable, GigyaLoginCallback gigyaLoginCallback) {
        final Class<Provider> providerClazz = getProviderClass(name);

        final IoCContainer tempContainer = _container.clone()
                .bind(GigyaLoginCallback.class, gigyaLoginCallback)
                .bind(IApiObservable.class, observable);
        try {
            return tempContainer.createInstance(providerClazz);
        } catch (Exception e) {
            // TODO: #baryo need to think what to do
            return null;
        } finally {
            tempContainer.dispose();
        }
    }

    private Class getProviderClass(String providerName) {
        if (providerName != null) {
            switch (providerName) {
                case FACEBOOK:
                    if (FacebookProvider.isAvailable(_fileUtils)) {
                        return FacebookProvider.class;
                    }
                    break;
                case GOOGLE:
                    if (GoogleProvider.isAvailable(_context)) {
                        return GoogleProvider.class;
                    }
                    break;
                case LINE:
                    if (LineProvider.isAvailable(_fileUtils)) {
                        return LineProvider.class;
                    }
                    break;
                case WECHAT:
                    if (WeChatProvider.isAvailable(_context, _fileUtils)) {
                        return WeChatProvider.class;
                    }
                    break;
            }
        }

        return WebLoginProvider.class;
    }

    @Override
    public void logoutFromUsedSocialProviders() {
        final IProvider[] usedProviders = getUsedSocialProviders();
        if (usedProviders.length > 0) {
            for (IProvider provider : usedProviders) {
                provider.logout();
            }

            _psService.removeSocialProviders();
        }
    }

    private IProvider[] getUsedSocialProviders() {
        final Set<IProvider> usedProviders = new HashSet<>();
        final Set<String> usedProvidersNames = _psService.getSocialProviders();
        for (String name : usedProvidersNames) {
            final IProvider provider = providerFor(name, new ApiObservable(), new GigyaLoginCallback() {
                @Override
                public void onSuccess(Object obj) {
                    // Redundant.
                }

                @Override
                public void onError(GigyaError error) {
                    // Redundant.
                }
            });

            // Make sure were not getting the web view provider.
            if (provider.getName().equals(name)) {
                usedProviders.add(provider);
            }
        }

        IProvider[] array = new IProvider[usedProviders.size()];
        return usedProviders.toArray(array);
    }
}
