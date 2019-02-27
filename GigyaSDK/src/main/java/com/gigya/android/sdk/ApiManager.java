package com.gigya.android.sdk;

import com.gigya.android.sdk.api.AnonymousApi;
import com.gigya.android.sdk.api.SdkConfigApi;
import com.gigya.android.sdk.api.account.FinalizeRegistrationApi;
import com.gigya.android.sdk.api.account.GetAccountApi;
import com.gigya.android.sdk.api.account.GetConflictingAccountApi;
import com.gigya.android.sdk.api.account.LoginApi;
import com.gigya.android.sdk.api.account.LogoutApi;
import com.gigya.android.sdk.api.account.NotifyLoginApi;
import com.gigya.android.sdk.api.account.RefreshProviderSessionApi;
import com.gigya.android.sdk.api.account.RegisterApi;
import com.gigya.android.sdk.api.account.ResetPasswordApi;
import com.gigya.android.sdk.api.account.SetAccountApi;
import com.gigya.android.sdk.log.GigyaLogger;
import com.gigya.android.sdk.login.LoginProvider;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.model.GigyaAccount;
import com.gigya.android.sdk.model.SessionInfo;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaInterceptionCallback;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;
import com.gigya.android.sdk.utils.ObjectUtils;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("WeakerAccess") // Access should remain public for client use.
public class ApiManager {

    public static final String LOG_TAG = "ApiManager";

    private NetworkAdapter _networkAdapter;
    private SessionManager _sessionManager;
    private AccountManager _accountManager;

    public ApiManager(NetworkAdapter networkAdapter,
                      SessionManager sessionManager, AccountManager accountManager) {
        _networkAdapter = networkAdapter;
        _sessionManager = sessionManager;
        _accountManager = accountManager;
    }

    public void loadConfig(final Runnable completionHandler) {
        final Configuration configuration = _sessionManager.getConfiguration();
        new SdkConfigApi(_networkAdapter, _sessionManager).call(new GigyaCallback<SdkConfigApi.SdkConfig>() {
            @Override
            public void onSuccess(SdkConfigApi.SdkConfig obj) {
                if (configuration.hasApiKey()) {
                    configuration.setIDs(obj.getIds());
                    configuration.setAppIds(obj.getAppIds());
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

    public void sendAnonymous(String api, Map<String, Object> params, final GigyaCallback<GigyaResponse> callback) {
        final Configuration configuration = _sessionManager.getConfiguration();
        if (!configuration.hasApiKey()) {
            GigyaLogger.error(LOG_TAG, "Configuration invalid. Api-Key unavailable");
            return;
        }
        new AnonymousApi<GigyaResponse>(_networkAdapter, _sessionManager, _accountManager)
                .call(api, params, callback);
    }

    public <H> void sendAnonymous(String api, Map<String, Object> params, Class<H> clazz, GigyaCallback<H> callback) {
        final Configuration configuration = _sessionManager.getConfiguration();
        if (!configuration.hasApiKey()) {
            GigyaLogger.error(LOG_TAG, "Configuration invalid. Api-Key unavailable");
            return;
        }
        new AnonymousApi<>(_networkAdapter, _sessionManager, _accountManager, clazz)
                .call(api, params, callback);
    }

    public void logout() {
        new LogoutApi(_networkAdapter, _sessionManager).call();
    }

    @SuppressWarnings("unchecked")
    public <T extends GigyaAccount> void login(Map<String, Object> params, GigyaLoginCallback callback) {
        _accountManager.invalidateAccount();
        new LoginApi<T>(_networkAdapter, _sessionManager, _accountManager,
                _accountManager.getAccountClazz()).call(params, callback);
    }

    @SuppressWarnings("unchecked")
    public <T extends GigyaAccount> void getAccount(GigyaCallback callback) {
        if (_accountManager.getCachedAccount()) {
            /* Always return a deep copy. */
            callback.onSuccess(ObjectUtils.deepCopy(new Gson(), _accountManager.getAccount(),
                    _accountManager.getAccountClazz()));
            return;
        }
        _accountManager.nextAccountInvalidationTimestamp();
        new GetAccountApi<T>(_networkAdapter, _sessionManager, _accountManager, _accountManager.getAccountClazz()).call(callback);
    }

    @SuppressWarnings("unchecked")
    public <T extends GigyaAccount> void setAccount(T account, GigyaCallback callback) {
        new SetAccountApi<>(_networkAdapter, _sessionManager, _accountManager,
                _accountManager.getAccountClazz(), account, _accountManager.getAccount()).call(callback);
    }

    @SuppressWarnings("unchecked")
    public <T extends GigyaAccount> void register(Map<String, Object> params, RegisterApi.RegisterPolicy policy, boolean finalize, GigyaLoginCallback<T> callback) {
        _accountManager.invalidateAccount();
        new RegisterApi<T>(_networkAdapter, _sessionManager, _accountManager, _accountManager.getAccountClazz(), policy, finalize).call(params, callback);
    }

    @SuppressWarnings("unchecked")
    public <T extends GigyaAccount> void finalizeRegistration(String regToken, GigyaLoginCallback<T> callback, Runnable completionHandler) {
        new FinalizeRegistrationApi<T>(_networkAdapter, _sessionManager, _accountManager)
                .call(regToken, callback, completionHandler);
    }

    @SuppressWarnings("unchecked")
    public <T extends GigyaAccount> void notifyLogin(String providerSessions, GigyaLoginCallback<T> callback, final Runnable completionHandler) {
        new NotifyLoginApi<T>(_networkAdapter, _sessionManager, _accountManager, _accountManager.getAccountClazz())
                .call(providerSessions, callback, new GigyaInterceptionCallback<T>() {
                    @Override
                    public void intercept(T obj) {
                        if (completionHandler != null) {
                            completionHandler.run();
                        }
                    }
                });
    }

    @SuppressWarnings("unchecked")
    public <T extends GigyaAccount> void notifyLogin(SessionInfo sessionInfo, final GigyaCallback<T> callback, final Runnable completionHandler) {
        new NotifyLoginApi<T>(_networkAdapter, _sessionManager, _accountManager, _accountManager.getAccountClazz())
                .call(sessionInfo, callback, new GigyaInterceptionCallback<T>() {
                    @Override
                    public void intercept(T obj) {
                        if (completionHandler != null) {
                            completionHandler.run();
                        }
                    }
                });
    }

    public void updateProviderSessions(String providerSession, final LoginProvider.LoginPermissionCallbacks permissionCallbacks) {
        new RefreshProviderSessionApi(_networkAdapter, _sessionManager)
                .call(providerSession, new GigyaCallback<GigyaResponse>() {
                    @Override
                    public void onSuccess(GigyaResponse obj) {
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

    public void getConflictingAccounts(String regToken, GigyaCallback<GigyaResponse> callback) {
        new GetConflictingAccountApi(_networkAdapter, _sessionManager).call(regToken, callback);
    }

    public void forgotPassword(String email, GigyaCallback<GigyaResponse> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("loginID", email);
        new ResetPasswordApi(_networkAdapter, _sessionManager).call(params, callback);
    }
}
