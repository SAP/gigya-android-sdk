package com.gigya.android.sdk.api.account_dep;

@Deprecated
public class FinalizeRegistrationApi { //<T extends GigyaAccount> extends BaseApi<T> {

//    private static final String API = "accounts.finalizeRegistration";
//
//    private final Class<T> clazz;
//    private final AccountManager accountManager;
//    private Runnable completionHandler;
//
//    public FinalizeRegistrationApi(NetworkAdapter networkAdapter, SessionManager sessionManager, AccountManager accountManager) {
//        super(networkAdapter, sessionManager);
//        this.accountManager = accountManager;
//        this.clazz = accountManager.getAccountClazz();
//    }
//
//    public void call(String regToken, GigyaCallback<T> callback, Runnable completionHandler) {
//        this.completionHandler = completionHandler;
//        Map<String, Object> params = new HashMap<>();
//        params.put("regToken", regToken);
//        params.put("include", "profile,data,emails,subscriptions,preferences");
//        params.put("includeUserInfo", "true");
//        GigyaApiRequest request = new GigyaApiRequestBuilder(sessionManager)
//                .api(API)
//                .params(params)
//                .build();
//        sendRequest(request, API, callback);
//    }
//
//    @Override
//    @SuppressWarnings("unchecked")
//    protected void onRequestSuccess(String api, GigyaApiResponse response, GigyaCallback<T> callback) {
//        if (response.contains("sessionInfo") && sessionManager != null) {
//            SessionInfo session = response.getField("sessionInfo", SessionInfo.class);
//            sessionManager.setSession(session);
//        }
//        final String json = response.asJson();
//        final T parsed = response.getGson().fromJson(json, clazz);
//        accountManager.setAccount(ObjectUtils.deepCopy(response.getGson(), parsed, clazz));
//        completionHandler.run();
//        callback.onSuccess(parsed);
//    }
}
