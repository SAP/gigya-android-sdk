package com.gigya.android.sdk.network.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.model.GigyaAccount;
import com.gigya.android.sdk.model.SessionInfo;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaInterceptionCallback;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.INetworkCallbacks;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import org.json.JSONObject;

import java.util.Map;

import static com.gigya.android.sdk.network.GigyaResponse.OK;

@SuppressWarnings("unchecked")
public class RegisterApi<T> extends BaseApi<T> {

    public enum RegisterPolicy {
        EMAIL, USERNAME, EMAIL_OR_USERNAME
    }

    private static final String API_INIT_REGISTRATION = "accounts.initRegistration";
    private static final String API_REGISTER = "accounts.register";
    private static final String API_FINALIZE_REGISTRATION = "accounts.finalizeRegistration";

    private final boolean finalize;
    private final RegisterPolicy policy;

    public RegisterApi(@NonNull Configuration configuration, @NonNull NetworkAdapter networkAdapter, @Nullable SessionManager sessionManager, @Nullable Class<T> clazz,
                       RegisterPolicy policy,
                       boolean finalize) {
        super(configuration, networkAdapter, sessionManager, clazz);
        this.finalize = finalize;
        this.policy = policy;
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

    public void call(final Map<String, Object> params, final GigyaCallback callback, final GigyaInterceptionCallback interceptor) {
        updateRegisterPolicy(params);
        GigyaRequest request = new GigyaRequestBuilder(configuration).sessionManager(sessionManager).api(API_INIT_REGISTRATION).build();
        networkAdapter.send(request, new INetworkCallbacks() {
            @Override
            public void onResponse(String jsonResponse) {
                if (callback == null) {
                    return;
                }
                try {
                    final GigyaResponse response = new GigyaResponse(new JSONObject(jsonResponse));
                    final int statusCode = response.getStatusCode();
                    if (statusCode == OK) {
                        final String regToken = (String) response.getField("regToken");
                        if (regToken == null) {
                            callback.onError(GigyaError.generalError());
                            return;
                        }
                        params.put("regToken", regToken);
                        params.put("finalizeRegistration", finalize);
                        /* Chain actual registration request. */
                        callSendRegistration(params, callback, interceptor);
                        return;
                    }
                    final int errorCode = response.getErrorCode();
                    final String localizedMessage = response.getErrorDetails();
                    final String callId = response.getCallId();
                    callback.onError(new GigyaError(errorCode, localizedMessage, callId));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    callback.onError(GigyaError.generalError());
                }
            }

            @Override
            public void onError(GigyaError gigyaError) {
                if (callback != null) {
                    callback.onError(gigyaError);
                }
            }
        });
    }

    private void callSendRegistration(final Map<String, Object> params, final GigyaCallback callback, final GigyaInterceptionCallback interceptor) {
        final GigyaRequest request = new GigyaRequestBuilder(configuration)
                .sessionManager(sessionManager).params(params).api(API_REGISTER).build();
        networkAdapter.send(request, new INetworkCallbacks() {
            @Override
            public void onResponse(String jsonResponse) {
                if (callback == null) {
                    return;
                }
                try {
                    final GigyaResponse response = new GigyaResponse(new JSONObject(jsonResponse));
                    final int statusCode = response.getStatusCode();
                    if (statusCode == OK) {
                        /* Update session info */
                        if (response.contains("sessionInfo") && sessionManager != null) {
                            SessionInfo session = response.getField("sessionInfo", SessionInfo.class);
                            sessionManager.setSession(session);
                        }
                        params.clear(); /* Clear sensitive data once it is not required. */
                        if (interceptor != null) {
                            T interception = (T) response.getGson().fromJson(jsonResponse, clazz != null ? clazz : GigyaAccount.class);
                            interceptor.intercept(interception);
                        }
                        final T parsed = (T) response.getGson().fromJson(jsonResponse, clazz != null ? clazz : GigyaAccount.class);
                        callback.onSuccess(parsed);
                        return;
                    }
                    final int errorCode = response.getErrorCode();
                    final String localizedMessage = response.getErrorDetails();
                    final String callId = response.getCallId();
                    callback.onError(new GigyaError(errorCode, localizedMessage, callId));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    callback.onError(GigyaError.generalError());
                }
            }

            @Override
            public void onError(GigyaError gigyaError) {
                if (callback != null) {
                    callback.onError(gigyaError);
                }
            }
        });
    }
}
