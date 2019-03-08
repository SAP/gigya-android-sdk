package com.gigya.android.sdk.api.account_dep;

@Deprecated
public class GetConflictingAccountApi {//extends BaseApi<GigyaApiResponse> {

//    private static final String API = "accounts.getConflictingAccount";
//
//    public GetConflictingAccountApi(NetworkAdapter networkAdapter, SessionManager sessionManager) {
//        super(networkAdapter, sessionManager);
//    }
//
//    public void call(String regToken, final GigyaCallback<GigyaApiResponse> callback) {
//        Map<String, Object> params = new HashMap<>();
//        params.put("regToken", regToken);
//        GigyaApiRequest request = new GigyaApiRequestBuilder(sessionManager).api(API).params(params).build();
//        sendRequest(request, API, callback);
//    }
//
//    @Override
//    protected void onRequestSuccess(String api, GigyaApiResponse response, GigyaCallback<GigyaApiResponse> callback) {
//        callback.onSuccess(response);
//    }
//
//    public static class ConflictingAccount {
//        private ArrayList<String> loginProviders = new ArrayList<>();
//        private String loginID;
//
//        public ArrayList<String> getLoginProviders() {
//            return loginProviders;
//        }
//
//        public String getLoginID() {
//            return loginID;
//        }
//    }
}
