package com.gigya.android.sdk.api.account_dep;

import com.gigya.android.sdk.model.account.GigyaAccount;

@Deprecated
public class NotifyLoginApi<T extends GigyaAccount> {

//    private final Class<T> clazz;
//
//    public NotifyLoginApi(NetworkAdapter networkAdapter, SessionManager sessionManager, AccountManager accountManager) {
//        super(networkAdapter, sessionManager, accountManager);
//        this.clazz = accountManager.getAccountClazz();
//    }
//
//    private static final String API = "socialize.notifyLogin";
//
//    public void call(SessionInfo sessionInfo, final GigyaCallback<T> callback, final GigyaInterceptionCallback<T> interceptor) {
//        // Update session.
//        if (sessionManager != null) {
//            sessionManager.setSession(sessionInfo);
//        }
//        // Request account info.
//        getAccount(callback, interceptor);
//    }
//
//    private GigyaInterceptionCallback<T> interceptor;
//
//    public void call(String providerSessions, final GigyaLoginCallback<T> callback, final GigyaInterceptionCallback<T> interceptor) {
//        this.interceptor = interceptor;
//        final Map<String, Object> params = new HashMap<>();
//        params.put("providerSessions", providerSessions);
//        GigyaApiRequest request = new GigyaApiRequestBuilder(sessionManager).params(params).api(API).build();
//        sendRequest(request, API, callback);
//    }
//
//    @Override
//    protected void onRequestSuccess(String api, GigyaApiResponse response, GigyaCallback<T> callback) {
//        if (sessionManager != null && response.contains("sessionSecret") && response.contains("sessionToken")) {
//            updateSessionAndRequestAccountInfo(response, callback, this.interceptor);
//        }
//    }
//
//    @Override
//    protected void onRequestError(String api, GigyaApiResponse response, GigyaCallback<T> callback) {
//        handleInterruptionError(response, (GigyaLoginCallback) callback);
//    }
//
//    private void updateSessionAndRequestAccountInfo(GigyaApiResponse notifyResponse, final GigyaCallback<T> callback,
//                                                    final GigyaInterceptionCallback<T> interceptor) {
//        /* Parse session info and request account info. */
//        SessionInfo sessionInfo;
//        final String sessionSecret = notifyResponse.getField("sessionSecret", String.class);
//        final String sessionToken = notifyResponse.getField("sessionToken", String.class);
//        final Long expirationTime = notifyResponse.getField("expirationTime", Long.class);
//        if (expirationTime != null) {
//            sessionInfo = new SessionInfo(sessionSecret, sessionToken, expirationTime);
//        } else {
//            sessionInfo = new SessionInfo(sessionSecret, sessionToken);
//        }
//        // Update session.
//        if (sessionManager != null) {
//            sessionManager.setSession(sessionInfo);
//        }
//        // Request account info.
//        getAccount(callback, interceptor);
//    }
//
//    private void getAccount(final GigyaCallback<T> callback, final GigyaInterceptionCallback<T> interceptor) {
//        // Request account info.
//        new GetAccountApi<>(networkAdapter, sessionManager, accountManager)
//                .call(new GigyaCallback<T>() {
//                    @Override
//                    public void onSuccess(T obj) {
//                        if (interceptor != null) {
//                            interceptor.intercept(obj);
//                        }
//                        callback.onSuccess(obj);
//                    }
//
//                    @Override
//                    public void onError(GigyaError error) {
//                        callback.onError(error);
//                    }
//                });
//    }
}
