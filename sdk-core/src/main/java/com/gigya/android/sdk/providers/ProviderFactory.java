package com.gigya.android.sdk.providers;

import android.content.Context;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.providers.external.ExternalProvider;
import com.gigya.android.sdk.providers.external.IProviderWrapper;
import com.gigya.android.sdk.providers.provider.IProvider;
import com.gigya.android.sdk.providers.provider.Provider;
import com.gigya.android.sdk.providers.provider.ProviderCallback;
import com.gigya.android.sdk.providers.provider.SSOProvider;
import com.gigya.android.sdk.providers.provider.WebLoginProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.gigya.android.sdk.GigyaDefinitions.Providers.SSO;

public class ProviderFactory implements IProviderFactory {
    final private IoCContainer _container;
    final private Context _context;
    final private Config _config;
    final private IPersistenceService _psService;

    private static final String LOG_TAG = "ProviderFactory";

    // Available external provider list. Lowercase.
    final private List<String> optionalProviders =
            Arrays.asList("facebook", "google", "googleplus", "line", "wechat");

    public ProviderFactory(IoCContainer container,
                           Context context,
                           Config config,
                           IPersistenceService persistenceService) {
        _container = container;
        _context = context;
        _config = config;
        _psService = persistenceService;
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Provider providerFor(String name, ProviderCallback providerCallback) {
        final IoCContainer tempContainer =
                _container.clone();
        if (providerCallback != null) {
            tempContainer.bind(ProviderCallback.class, providerCallback);
        }

        try {
            if (isExternalProvider(name)) {
                final ExternalProvider externalProvider = tempContainer.createInstance(ExternalProvider.class);
                final String rootPath = ExternalProvider.getPath();
                externalProvider.setProviderName(name);
                externalProvider.setProvidersRoot(rootPath);
                externalProvider.init(_container);
                final Class externalProviderClazz = ExternalProvider.getWrapperClass(_context, name, rootPath);
                if (externalProviderClazz != null) {
                    _container.bind(externalProviderClazz, externalProvider.getWrapper());
                }
                return externalProvider;
            } else {
                final Class<Provider> providerClazz = getProviderClass(name);
                final Provider provider = tempContainer.createInstance(providerClazz);
                // Bind generated provider to main IoC container.
                _container.bind(providerClazz, provider);
                return provider;
            }
        } catch (Exception e) {
            GigyaLogger.error(LOG_TAG, "Error instantiating selected social provider");
            return null;
        } finally {
            tempContainer.dispose();
        }
    }

    /***
     * No longer in use.
     * External provider wrapper code is not a part of the client application.
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Provider usedProviderFor(String name) {
        Class<Provider> providerClazz;
        try {
            if (isExternalProvider(name)) {
                final String rootPath = ExternalProvider.getPath();
                providerClazz = ExternalProvider.getWrapperClass(_context, name, rootPath);
            } else {
                providerClazz = getProviderClass(name);
            }
            return _container.get(providerClazz);
        } catch (Exception e) {
            GigyaLogger.error(LOG_TAG, "Error instantiating used provider");
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    private Class getProviderClass(String providerName) {
        if (providerName != null) {
            if (SSO.equals(providerName)) {
                return SSOProvider.class;
            }
        }
        return WebLoginProvider.class;
    }

    /**
     * Iterate through relevant social provider wrappers (this applies for external SDK specific providers and
     * not social login performed via web screen-sets) and attempt to logout of provider.
     */
    @Override
    public void logoutFromUsedSocialProviders() {
        final ArrayList<IProviderWrapper> usedProviders = getUsedSocialProviders();
        if (usedProviders.size() > 0) {
            for (IProviderWrapper provider : usedProviders) {
                provider.logout();
            }
            _psService.removeSocialProviders();
        }
    }

    /**
     * Get the available list of used External social provider wrappers.
     * Method is used for logout attempt.
     *
     * @return List of used IProviderWrapper interfaces.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private ArrayList<IProviderWrapper> getUsedSocialProviders() {
        ArrayList<IProviderWrapper> providers = new ArrayList<>();

        final String root = ExternalProvider.getPath();
        for (String optional : optionalProviders) {
            try {
                Class providerClass = ExternalProvider.getWrapperClass(_context, optional, root);
                if (_container.isBound(providerClass)) {
                    IProviderWrapper provider = (IProviderWrapper) _container.get(providerClass);
                    if (provider != null) {
                        providers.add(provider);
                    }
                }
            } catch (Exception e) {
                GigyaLogger.error(LOG_TAG, "getUsedSocialProviders: " + e.getLocalizedMessage());
            }
        }
        return providers;
    }

    public boolean isExternalProvider(String provider) {
        return optionalProviders.contains(provider.toLowerCase());
    }
}
