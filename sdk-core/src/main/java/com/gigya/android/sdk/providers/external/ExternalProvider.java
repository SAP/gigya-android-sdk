package com.gigya.android.sdk.providers.external;

import android.content.Context;

import androidx.annotation.Nullable;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.providers.provider.Provider;
import com.gigya.android.sdk.providers.provider.ProviderCallback;
import com.gigya.android.sdk.session.SessionInfo;

import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class ExternalProvider extends Provider {

    private static final String LOG_TAG = "ProviderManager";

    private IoCContainer providerContainer;

    private IProviderWrapper wrapper;

    private final Context _context;

    private String name;
    private String providersRoot;

    public ExternalProvider(Context context,
                            IPersistenceService persistenceService,
                            ProviderCallback providerCallback) {
        super(context, persistenceService, providerCallback);
        _context = context;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Nullable
    public IProviderWrapper getWrapper() {
        try {
            final Class clazz = getWrapperClass(_context, name, providersRoot);
            if (clazz != null) {
                final IProviderWrapper wrapper = (IProviderWrapper) providerContainer.get(clazz);
                return wrapper;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void init(IoCContainer container) {
        providerContainer = container.clone();
        wrapper = instantiateProvider(getName());
    }

    /***
     * Set the external provider name.
     * @param name
     */
    public void setProviderName(String name) {
        // Temporary fix for legacy google provider.
        if (name.equals("googleplus")) {
            name = "google";
        }
        this.name = name;
    }

    /***
     * Set the root path for the external provider files.
     * @param providersRoot Files Path in project.
     */
    public void setProvidersRoot(String providersRoot) {
        this.providersRoot = providersRoot;
    }

    /**
     * Get the correct provider wrapper class.
     *
     * @param provider Provider name.
     */
    @SuppressWarnings("rawtypes")
    public static Class getWrapperClass(Context context, String provider, String root) throws ClassNotFoundException {
        // Try to get wrapper from absolute root.
        final String providerName = provider.substring(0, 1).toUpperCase() + provider.substring(1);
        final String absoluteRootClassName = root + "." + providerName + "ProviderWrapper";
        try {
            return Class.forName(absoluteRootClassName);
        } catch (Exception ex1) {
            GigyaLogger.debug(LOG_TAG, "Unable to get provider class from absolute root path");
            // Try to get wrapper from package/root combination.
            final String packageName = context.getPackageName();
            final String className = packageName + "." + root + "." + providerName + "ProviderWrapper";
            try {
                return Class.forName(className);
            } catch (Exception ex2) {
                ex2.printStackTrace();
                GigyaLogger.debug(LOG_TAG, "Unable to get provider class from package/root combination path");
                GigyaLogger.error(LOG_TAG, "Error getting provider class name. Check your path");
                return null;
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Nullable
    private IProviderWrapper instantiateProvider(String provider) {
        try {
            final Class clazz = getWrapperClass(_context, provider, providersRoot);
            if (clazz != null) {
                IProviderWrapper wrapper = (IProviderWrapper) providerContainer.get(clazz);
                if (wrapper == null) {
                    wrapper = (IProviderWrapper) providerContainer.createInstance(clazz);
                }
                providerContainer.bind(clazz, wrapper);
                return wrapper;
            }
        } catch (Exception ex) {
            GigyaLogger.error(LOG_TAG, "Error instantiating " + provider + "wrapper class");
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Generate the relevant provider sessions object.
     */
    private String generateProviderSessions(String provider, Map<String, Object> providerLoginMap) {
        try {
            switch (provider.toLowerCase()) {
                case "facebook":
                    final String facebookToken = (String) providerLoginMap.get("token");
                    final long facebookExpiration = (long) providerLoginMap.get("expiration");
                    return new JSONObject()
                            .put("facebook", new JSONObject()
                                    .put("authToken", facebookToken).put("tokenExpiration", facebookExpiration)).toString();
                case "google":
                case "googleplus":
                    final String idToken = (String) providerLoginMap.get("idToken");
                    return new JSONObject()
                            .put(provider, new JSONObject()
                                    .put("idToken", idToken)).toString();
                case "line":
                    final String lineToken = (String) providerLoginMap.get("token");
                    return new JSONObject()
                            .put(provider, new JSONObject()
                                    .put("authToken", lineToken)).toString();
                case "wechat":
                    final String wechatCode = (String) providerLoginMap.get("code");
                    final String uid = (String) providerLoginMap.get("uid");
                    return new JSONObject()
                            .put(provider, new JSONObject()
                                    .put("authToken", wechatCode).put("providerUID", uid)).toString();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void login(final Map<String, Object> loginParams, final String loginMode) {
        if (wrapper == null) {
            //TODO: Error - missing provider.
            GigyaLogger.error(LOG_TAG, "Requested login from provider that cannot be instantiated");
            return;
        }

        wrapper.login(_context, loginParams, new IProviderWrapperCallback() {
            @Override
            public void onLogin(Map<String, Object> wrapperLoginParams) {
                // Transform login parameters to relevant provider sessions.
                String providerSessions = generateProviderSessions(name, wrapperLoginParams);
                // Continue login flow giving success with correct parameters.
                wrapperLoginParams.putAll(loginParams);
                onLoginSuccess(wrapperLoginParams, providerSessions, loginMode);
            }

            @Override
            public void onCanceled() {
                ExternalProvider.this.onCanceled();
            }

            @Override
            public void onFailed(String withError) {
                onLoginFailed(withError);
            }

        });
    }

    @Override
    public void logout() {
        IProviderWrapper wrapper = instantiateProvider(name);
        if (wrapper == null) {
            // Error missing provider.
            GigyaLogger.error(LOG_TAG, "Requested logout from provider that cannot be instantiated");
            return;
        }
        wrapper.logout();
    }

    @Override
    @Deprecated
    public String getProviderSessions(String tokenOrCode, long expiration, String uid) {
        // Remove after clearing internal provider classes.
        return null;
    }

    // External providers path is currently strict.
    public static String getPath() {
        return "gigya.providers";
    }

}
