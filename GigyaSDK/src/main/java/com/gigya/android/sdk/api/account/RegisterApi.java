package com.gigya.android.sdk.api.account;

import android.support.annotation.NonNull;

import com.gigya.android.sdk.AccountManager;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.api.InterruptionEnabledApi;
import com.gigya.android.sdk.model.GigyaAccount;
import com.gigya.android.sdk.model.SessionInfo;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;
import com.gigya.android.sdk.utils.ObjectUtils;

import java.util.Map;

@SuppressWarnings("unchecked")
public class RegisterApi<T extends GigyaAccount> extends InterruptionEnabledApi<T> {

    public enum RegisterPolicy {
        EMAIL, USERNAME, EMAIL_OR_USERNAME
    }

    private static final String API_INIT_REGISTRATION = "accounts.initRegistration";
    private static final String API_REGISTER = "accounts.register";
    private static final String API_FINALIZE_REGISTRATION = "accounts.finalizeRegistration";

    private boolean finalize;
    private RegisterPolicy policy;
    private final Class<T> clazz;

    public RegisterApi(NetworkAdapter networkAdapter, SessionManager sessionManager, AccountManager accountManager, @NonNull Class<T> clazz,
                       RegisterPolicy policy,
                       boolean finalize) {
        super(networkAdapter, sessionManager, accountManager);
        this.clazz = clazz;
        this.finalize = finalize;
        this.policy = policy;
    }

    public RegisterApi(NetworkAdapter networkAdapter, SessionManager sessionManager, AccountManager accountManager, @NonNull Class<T> clazz) {
        super(networkAdapter, sessionManager, accountManager);
        this.clazz = clazz;
    }

    private Map<String, Object> params;

    public void call(final Map<String, Object> params, final GigyaLoginCallback callback) {
        this.params = params;
        updateRegisterPolicy(params);
        GigyaRequest request = new GigyaRequestBuilder(sessionManager).api(API_INIT_REGISTRATION).build();
        sendRequest(request, API_INIT_REGISTRATION, callback);
    }

    private void callSendRegistration(final Map<String, Object> params, final GigyaLoginCallback callback) {
        final GigyaRequest request = new GigyaRequestBuilder(sessionManager).params(params).api(API_REGISTER).build();
        sendRequest(request, API_REGISTER, callback);
    }

    @Override
    protected void onRequestSuccess(String api, GigyaResponse response, GigyaCallback<T> callback) {
        switch (api) {
            case API_INIT_REGISTRATION:
                final String regToken = (String) response.getField("regToken");
                if (regToken == null) {
                    callback.onError(GigyaError.generalError());
                    return;
                }
                params.put("regToken", regToken);
                params.put("finalizeRegistration", finalize);
                /* Chain actual registration request. */
                callSendRegistration(params, (GigyaLoginCallback) callback);
                break;
            case API_REGISTER:
                if (response.contains("sessionInfo") && sessionManager != null) {
                    SessionInfo session = response.getField("sessionInfo", SessionInfo.class);
                    sessionManager.setSession(session);
                }
                params.clear(); /* Clear sensitive data once it is not required. */
                final String json = response.asJson();
                final T parsed = response.getGson().fromJson(json, clazz);
                accountManager.setAccount(ObjectUtils.deepCopy(response.getGson(), parsed, clazz));
                callback.onSuccess(parsed);
                break;
        }
    }

    @Override
    protected void onRequestError(String api, GigyaResponse response, GigyaCallback<T> callback) {
        handleInterruptionError(response, (GigyaLoginCallback) callback);
    }

    private void updateRegisterPolicy(Map<String, Object> params) {
        final String loginId = (String) params.get("loginID");
        if (loginId == null) {
            return;
        }
        params.remove("loginID");
        switch (this.policy) {
            case EMAIL:
                params.put("email", loginId);
                break;
            case USERNAME:
            case EMAIL_OR_USERNAME:
                params.put("username", loginId);
                break;
        }
    }

}
