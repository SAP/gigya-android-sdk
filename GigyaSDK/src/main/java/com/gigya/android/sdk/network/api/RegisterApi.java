package com.gigya.android.sdk.network.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaRegisterCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.model.GigyaAccount;
import com.gigya.android.sdk.model.SessionInfo;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaInterceptionCallback;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.network.GigyaRequestBuilderOld;
import com.gigya.android.sdk.network.GigyaRequestOld;
import com.gigya.android.sdk.network.GigyaRequestQueue;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.INetworkCallbacks;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import org.json.JSONObject;

import java.util.Map;

import static com.gigya.android.sdk.network.GigyaResponse.OK;

@SuppressWarnings("unchecked")
public class RegisterApi<T> extends BaseApi<T> implements IApi {

    public enum RegisterPolicy {
        EMAIL, USERNAME, EMAIL_OR_USERNAME
    }

    private static final String API_INIT_REGISTRATION = "accounts.initRegistration";
    private static final String API_REGISTER = "accounts.register";
    private static final String API_FINALIZE_REGISTRATION = "accounts.finalizeRegistration";

    private final boolean finalize;
    private final RegisterPolicy policy;

    @Deprecated
    public RegisterApi(@NonNull Configuration configuration, @Nullable SessionManager sessionManager, @Nullable GigyaRequestQueue requestQueue, @Nullable Class<T> clazz,
                       RegisterPolicy policy,
                       boolean finalize) {
        super(configuration, sessionManager, requestQueue, clazz);
        this.finalize = finalize;
        this.policy = policy;
    }

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

    @Deprecated
    @SuppressWarnings("unchecked")
    @Override
    public GigyaRequestOld getRequest(final Map<String, Object> params, final GigyaCallback callback, final GigyaInterceptionCallback interceptor) {
        updateRegisterPolicy(params);
        return new GigyaRequestBuilderOld<InitRegistration>(configuration)
                .sessionManager(sessionManager)
                .api(API_INIT_REGISTRATION)
                .output(InitRegistration.class)
                .callback(new GigyaCallback<InitRegistration>() {
                    @Override
                    public void onSuccess(InitRegistration obj) {
                        final String regToken = obj.getRegToken();
                        params.put("regToken", regToken);
                        params.put("finalizeRegistration", finalize);
                        sendRegistration(configuration, params, callback, interceptor);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        callback.onError(error);
                    }
                })
                .build();
    }

    @Deprecated
    @SuppressWarnings("unchecked")
    private <T> void sendRegistration(final Configuration configuration, final Map<String, Object> params, final GigyaCallback<T> callback, final GigyaInterceptionCallback<T> interceptor) {
        final GigyaRequestOld request = new GigyaRequestBuilderOld(configuration)
                .sessionManager(sessionManager)
                .api(API_REGISTER)
                .params(params)
                .output(clazz)
                .interceptor(interceptor)
                .callback(callback)
                .callback(new GigyaRegisterCallback<T>() {

                    @Override
                    public void onSuccess(T obj) {
                        params.clear();
                        callback.onSuccess(obj);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        onRegistrationError(error, params, interceptor, callback);
                    }
                })
                .build();
        requestQueue.add(request);
    }

    @Deprecated
    @SuppressWarnings("unchecked")
    private <T> void onRegistrationError(GigyaError error, final Map<String, Object> params, GigyaInterceptionCallback<T> interceptor, GigyaCallback<T> callback) {
        final int errorCode = error.getErrorCode();
        switch (errorCode) {
            case GigyaError.Codes.ERROR_ACCOUNT_PENDING_REGISTRATION:
                ApiResolver<T> resolver = new ApiResolver.Builder()
                        .incident(ApiResolver.Incident.ACCOUNT_PENDING_REGISTRATION)
                        .queue(requestQueue)
                        .sessionMananger(sessionManager)
                        .interceptor(interceptor)
                        .params(params)
                        .callback(callback)
                        .build();
                final String regToken = (String) params.get("regToken");
                ((GigyaRegisterCallback) callback).onPendingRegistration(regToken, resolver);
                break;
            default:
                callback.onError(error);
                break;
        }
    }

    //region Flow specific classes

    @Deprecated
    private static class InitRegistration {

        private String regToken;

        public String getRegToken() {
            return regToken;
        }
    }

    //endregion
}
