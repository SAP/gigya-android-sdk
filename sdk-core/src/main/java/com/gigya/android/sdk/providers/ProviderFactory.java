package com.gigya.android.sdk.providers;

import static com.gigya.android.sdk.GigyaDefinitions.Providers.FACEBOOK;
import static com.gigya.android.sdk.GigyaDefinitions.Providers.GOOGLE;
import static com.gigya.android.sdk.GigyaDefinitions.Providers.LINE;
import static com.gigya.android.sdk.GigyaDefinitions.Providers.SSO;
import static com.gigya.android.sdk.GigyaDefinitions.Providers.WECHAT;

import android.content.Context;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.providers.provider.FacebookProvider;
import com.gigya.android.sdk.providers.provider.GoogleProvider;
import com.gigya.android.sdk.providers.provider.IProvider;
import com.gigya.android.sdk.providers.provider.LineProvider;
import com.gigya.android.sdk.providers.provider.Provider;
import com.gigya.android.sdk.providers.provider.ProviderCallback;
import com.gigya.android.sdk.providers.provider.SSOProvider;
import com.gigya.android.sdk.providers.provider.WeChatProvider;
import com.gigya.android.sdk.providers.provider.WebLoginProvider;
import com.gigya.android.sdk.utils.FileUtils;

import java.util.ArrayList;

public class ProviderFactory implements IProviderFactory {
    final private IoCContainer _container;
    final private Context _context;
    final private FileUtils _fileUtils;
    final private IPersistenceService _psService;

    private static final String LOG_TAG = "ProviderFactory";

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
    public Provider providerFor(String name, ProviderCallback providerCallback) {
        final Class<Provider> providerClazz = getProviderClass(name);

        final IoCContainer tempContainer =
                _container.clone()
                        .bind(ProviderCallback.class, providerCallback);
        try {
            final Provider provider = tempContainer.createInstance(providerClazz);
            // Bind generated provider to main IoC container.
            _container.bind(providerClazz, provider);
            return provider;
        } catch (Exception e) {
            GigyaLogger.error(LOG_TAG, "Missing dependency for creating provider");
            return null;
        } finally {
            tempContainer.dispose();
        }
    }

    @Override
    public Provider usedProviderFor(String name) {
        final Class<Provider> providerClazz = getProviderClass(name);
        try {
            return _container.get(providerClazz);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static final String LEGACY_GOOGLE_IDENTIFIER = "googleplus";

    private Class getProviderClass(String providerName) {
        if (providerName != null) {
            switch (providerName) {
                case FACEBOOK:
                    if (FacebookProvider.isAvailable(_fileUtils)) {
                        return FacebookProvider.class;
                    } else {
                        throw new RuntimeException("Facebook library implementation is a required dependency." +
                                " Please make sure it is correctly implemented in your build.gradle file.\n" +
                                "https://sap.github.io/gigya-android-sdk/sdk-core/#facebook");
                    }
                case GOOGLE:
                case LEGACY_GOOGLE_IDENTIFIER:
                    if (GoogleProvider.isAvailable(_context)) {
                        return GoogleProvider.class;
                    } else {
                        throw new RuntimeException("Google Play Services & Google auth library implementation is required." +
                                " Please make sure it is correctly implemented in your build.gradle file.\n" +
                                "https://sap.github.io/gigya-android-sdk/sdk-core/#google");
                    }
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
                case SSO:
                    return SSOProvider.class;
            }
        }

        return WebLoginProvider.class;
    }

    @Override
    public void logoutFromUsedSocialProviders() {
        final ArrayList<IProvider> usedProviders = getUsedSocialProviders();
        if (usedProviders.size() > 0) {
            for (IProvider provider : usedProviders) {
                provider.logout();
            }

            _psService.removeSocialProviders();
        }
    }

    private final String[] _optionalBoundProviders = new String[]{GOOGLE, FACEBOOK, LINE, WECHAT};

    @SuppressWarnings("rawtypes")
    private ArrayList<IProvider> getUsedSocialProviders() {
        ArrayList<IProvider> providers = new ArrayList<>();

        for (String optional : _optionalBoundProviders) {
            try {
                Class providerClass = getProviderClass(optional);
                if (_container.isBound(providerClass)) {
                    IProvider provider = (IProvider) _container.get(providerClass);
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
}
