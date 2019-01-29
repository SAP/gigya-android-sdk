package com.gigya.android.sdk;

import com.gigya.android.sdk.api.AnonymousApi;
import com.gigya.android.sdk.api.GetAccountApi;
import com.gigya.android.sdk.api.LoginApi;
import com.gigya.android.sdk.api.LogoutApi;
import com.gigya.android.sdk.api.NotifyLoginApi;
import com.gigya.android.sdk.api.RefreshProviderSessionApi;
import com.gigya.android.sdk.api.RegisterApi;
import com.gigya.android.sdk.api.SdkConfigApi;
import com.gigya.android.sdk.api.SetAccountApi;
import com.gigya.android.sdk.log.GigyaLogger;
import com.gigya.android.sdk.login.LoginProvider;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.model.SessionInfo;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaInterceptionCallback;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings("WeakerAccess") // Access should remain public for client use.
public class ApiManager<T> {

    public static final String LOG_TAG = "ApiManager";

    public Configuration _configuration;
    public NetworkAdapter _networkAdapter;
    public SessionManager _sessionManager;
    public AccountManager<T> _accountManager;

    public ApiManager() {
        DependencyRegistry.getInstance().inject(this);
    }

    public void inject(Configuration configuration, NetworkAdapter networkAdapter,
                       SessionManager sessionManager, AccountManager<T> accountManager) {
        _configuration = configuration;
        _networkAdapter = networkAdapter;
        _sessionManager = sessionManager;
        _accountManager = accountManager;
    }

    public void loadConfig(final Runnable completionHandler) {
        new SdkConfigApi(_configuration, _networkAdapter, _sessionManager).call(new GigyaCallback<SdkConfigApi.SdkConfig>() {
            @Override
            public void onSuccess(SdkConfigApi.SdkConfig obj) {
                if (_configuration.hasApiKey()) {
                    _configuration.setIDs(obj.getIds());
                    _configuration.setAppIds(obj.getAppIds());
                    if (_sessionManager != null) {
                        /* Trigger session save in order to keep track of GMID, UCID. */
                        _sessionManager.save();
                    }
                    _networkAdapter.release();
                }
                if (completionHandler != null) {
                    completionHandler.run();
                }
            }

            @Override
            public void onError(GigyaError error) {
                GigyaLogger.error(LOG_TAG, "getSDKConfig error: " + error.toString());
            }
        });
    }

    public void sendAnonymous(String api, Map<String, Object> params, GigyaCallback<GigyaResponse> callback) {
        if (!_configuration.hasApiKey()) {
            GigyaLogger.error(LOG_TAG, "Configuration invalid. Api-Key unavailable");
            return;
        }
        new AnonymousApi<GigyaResponse>(_configuration, _networkAdapter, _sessionManager)
                .call(api, params, callback);
    }

    public <H> void sendAnonymous(String api, Map<String, Object> params, Class<H> clazz, GigyaCallback<H> callback) {
        if (!_configuration.hasApiKey()) {
            GigyaLogger.error(LOG_TAG, "Configuration invalid. Api-Key unavailable");
            return;
        }
        new AnonymousApi<>(_configuration, _networkAdapter, _sessionManager, clazz)
                .call(api, params, callback);
    }

    public void logout() {
        new LogoutApi(_configuration, _networkAdapter, _sessionManager).call();
    }

    public void login(String username, String password, GigyaCallback<T> callback) {
        _accountManager.invalidateAccount();
        final TreeMap<String, Object> params = new TreeMap<>();
        params.put("loginID", username);
        params.put("password", password);
        params.put("include", "profile,data,subscriptions,preferences");
        new LoginApi<>(_configuration, _networkAdapter, _sessionManager, _accountManager.getAccountClazz())
                .call(params, callback, new GigyaInterceptionCallback<T>() {
                    @Override
                    public void intercept(T obj) {
                        DependencyRegistry.getInstance().getAccountManager().setAccount(obj);
                    }
                });
    }

    public void getAccount(GigyaCallback<T> callback) {
        if (_accountManager.getCachedAccount()) {
            callback.onSuccess(_accountManager.getAccount());
            return;
        }
        _accountManager.nextAccountInvalidationTimestamp();
        new GetAccountApi<>(_configuration, _networkAdapter, _sessionManager, _accountManager.getAccountClazz())
                .call(callback, new GigyaInterceptionCallback<T>() {
                    @Override
                    public void intercept(T obj) {
                        _accountManager.setAccount(obj);
                    }
                });
    }

    public void setAccount(T account, GigyaCallback<T> callback) {
        new SetAccountApi<>(_configuration, _networkAdapter, _sessionManager, _accountManager.getAccountClazz(), account, _accountManager.getAccount())
                .call(callback, new GigyaInterceptionCallback<T>() {
                    @Override
                    public void intercept(T obj) {
                        // Flush account cache.
                        _accountManager.setAccount(obj);
                    }
                });
    }

    public void register(Map<String, Object> params, RegisterApi.RegisterPolicy policy, boolean finalize, GigyaRegisterCallback<T> callback) {
        _accountManager.invalidateAccount();
        new RegisterApi<>(_configuration, _networkAdapter, _sessionManager, _accountManager.getAccountClazz(), policy, finalize)
                .call(params, callback, new GigyaInterceptionCallback<T>() {
                    @Override
                    public void intercept(T obj) {
                        // TODO: 18/12/2018 Should we call getAccountInfo here?
                    }
                });
    }

    public void notifyLogin(String providerSessions, GigyaCallback<T> callback, final Runnable completionHandler) {
        new NotifyLoginApi<>(_configuration, _networkAdapter, _sessionManager, _accountManager.getAccountClazz())
                .call(providerSessions, callback, new GigyaInterceptionCallback<T>() {
                    @Override
                    public void intercept(T obj) {
                        _accountManager.setAccount(obj);
                        if (completionHandler != null) {
                            completionHandler.run();
                        }
                    }
                });
    }

    public void notifyLogin(SessionInfo sessionInfo, final GigyaCallback<T> callback, final Runnable completionHandler) {
        new NotifyLoginApi<>(_configuration, _networkAdapter, _sessionManager, _accountManager.getAccountClazz())
                .call(sessionInfo, callback, new GigyaInterceptionCallback<T>() {
                    @Override
                    public void intercept(T obj) {
                        _accountManager.setAccount(obj);
                        if (completionHandler != null) {
                            completionHandler.run();
                        }
                    }
                });
    }

    public void updateProviderSessions(String providerSession, final LoginProvider.LoginPermissionCallbacks permissionCallbacks) {
        new RefreshProviderSessionApi(_configuration, _networkAdapter, _sessionManager)
                .call(providerSession, new GigyaCallback() {
                    @Override
                    public void onSuccess(Object obj) {
                        GigyaLogger.debug(LOG_TAG, "onProviderTrackingTokenChanges: Success - provider token updated");

                        if (permissionCallbacks != null) {
                            permissionCallbacks.granted();
                        }
                        /* Invalidate cached account. */
                        _accountManager.invalidateAccount();
                    }

                    @Override
                    public void onError(GigyaError error) {
                        GigyaLogger.debug(LOG_TAG, "onProviderTrackingTokenChanges: Error: " + error.getLocalizedMessage());

                        if (permissionCallbacks != null) {
                            permissionCallbacks.failed(error.getLocalizedMessage());
                        }
                    }
                });
    }
}
