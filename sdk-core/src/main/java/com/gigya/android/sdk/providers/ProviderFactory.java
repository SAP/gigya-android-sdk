package com.gigya.android.sdk.providers;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.providers.external.ExternalProvider;
import com.gigya.android.sdk.providers.external.IProviderWrapper;
import com.gigya.android.sdk.providers.external.ProviderWrapper;
import com.gigya.android.sdk.providers.provider.Provider;
import com.gigya.android.sdk.providers.provider.ProviderCallback;
import com.gigya.android.sdk.providers.provider.SSOProvider;
import com.gigya.android.sdk.providers.provider.WebLoginProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.gigya.android.sdk.GigyaDefinitions.Providers.SSO;

import androidx.annotation.Nullable;

public class ProviderFactory implements IProviderFactory {
    final private IoCContainer _container;
    final private Context _context;
    final private Config _config;
    final private IPersistenceService _psService;

    private static final String LOG_TAG = "ProviderFactory";

    // Available external provider list. Lowercase.
    final private List<String> optionalProviders =
            Arrays.asList("facebook", "google", "googleplus", "line", "wechat");

    // Default path is "gigya.providers" if not set manually.
    public String externalProviderPath = "gigya.providers";
    // Default meta-data key for external provider path if user chooses to add it.
    private static final String EXTERNAL_PROVIDERS_META_DATA_PATH_KEY = "com.gigya.android.externalProvidersPath";

    public ProviderFactory(IoCContainer container,
                           Context context,
                           Config config,
                           IPersistenceService persistenceService) {
        _container = container;
        _context = context;
        _config = config;
        _psService = persistenceService;
        checkMetaDataForCustomExternalProviderPath();
    }

    private void checkMetaDataForCustomExternalProviderPath() {
        String path;
        try {
            PackageManager packageManager = _context.getPackageManager();
            if (packageManager != null) {
                ApplicationInfo applicationInfo = packageManager.getApplicationInfo(_context.getPackageName(),
                        PackageManager.GET_META_DATA);
                if (applicationInfo != null) {
                    if (applicationInfo.metaData != null) {
                        path = String.valueOf(applicationInfo.metaData.get(EXTERNAL_PROVIDERS_META_DATA_PATH_KEY));
                        if (!TextUtils.isEmpty(path)) {
                            if (path.equals("null")) {
                                return;
                            }
                            externalProviderPath = path;
                            GigyaLogger.debug(LOG_TAG, "External provider path from meta-data = " + path);
                        }
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            GigyaLogger.debug(LOG_TAG, "External provider path from meta-data exception - not found");
            //e.printStackTrace();
        }
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
                // Must have fix for deprecated name "googleplus" until WebSDK will no longer save it.
                if (name.equals("googleplus")) {
                    name = "google";
                }
                final ExternalProvider externalProvider = tempContainer.createInstance(ExternalProvider.class);
                final String rootPath = externalProviderPath;
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
                final String rootPath = externalProviderPath;
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

    /**
     * @noinspection unchecked
     */
    @Nullable
    @Override
    public ProviderWrapper getProviderWrapper(String name) {
        Class<ProviderWrapper> providerClazz;
        try {
            if (!isExternalProvider(name)) return null;
            final String rootPath = externalProviderPath;
            providerClazz = ExternalProvider.getWrapperClass(_context, name, rootPath);
            if (providerClazz != null) {
                return _container.get(providerClazz);
            }
        } catch (Exception ex) {
            GigyaLogger.error(LOG_TAG, "Error instantiating used provider");
            ex.printStackTrace();
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

    @Override
    public void setExternalProvidersPath(String path) {
        this.externalProviderPath = path;
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

        final String root = externalProviderPath;
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
                // No point in logging exception if not in debug mode.
                // If an app does not use any social providers this may cause confusion in error logging.
                if (!optional.equals("googleplus")) {
                    GigyaLogger.debug(LOG_TAG, "getUsedSocialProviders: " + e.getLocalizedMessage());
                }
            }
        }
        return providers;
    }

    public boolean isExternalProvider(String provider) {
        return optionalProviders.contains(provider.toLowerCase());
    }

}
