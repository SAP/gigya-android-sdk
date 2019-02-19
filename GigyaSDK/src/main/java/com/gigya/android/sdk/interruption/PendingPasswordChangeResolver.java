package com.gigya.android.sdk.interruption;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaResponse;

import java.util.HashMap;
import java.util.Map;

public class PendingPasswordChangeResolver extends LoginResolver {

    private String regToken;

    public String getRegToken() {
        return regToken;
    }

    public PendingPasswordChangeResolver(GigyaLoginCallback loginCallback, String regToken) {
        super(loginCallback);
        this.regToken = regToken;
    }

    /**
     * Resolve Pending password state change using loginID.
     * Using this resolve will trigger a password reset email to be sent to the user's email.
     * Email field must be available in the user account for this resolve.
     *
     * @param loginId User loginID.
     */
    public void resolve(final String loginId) {
        Map<String, Object> params = new HashMap<>();
        params.put("LoginID", loginId);
        apiManager.resetPassword(params, new GigyaCallback<GigyaResponse>() {
            @Override
            public void onSuccess(GigyaResponse obj) {
                loginCallback.onResetPasswordLinkSent();
            }

            @Override
            public void onError(GigyaError error) {
                loginCallback.onError(error);
            }
        });
    }

    /**
     * Resolve Pending password state change using loginID, newPassword, secretAnswer parameters.
     * Using this resolve requires your site setup to use "passwordReset.requireSecurityCheck: true" in your site policy.
     *
     * @param params Parameter map.
     * @see <a href=https://developers.gigya.com/display/GD/Using+Security+Questions></a>
     */
    public void resolve(final Map<String, Object> params) {
        apiManager.resetPassword(params, new GigyaCallback<GigyaResponse>() {
            @Override
            public void onSuccess(GigyaResponse obj) {
                final String loginID = (String) params.get("loginID");
                final String newPassowrd = (String) params.get("newPassword");
                // Re-login.
                if (loginID != null && newPassowrd != null) {
                    Map<String, Object> loginMap = new HashMap<>();
                    loginMap.put("loginID", loginID);
                    loginMap.put("password", newPassowrd);
                    apiManager.login(loginMap, loginCallback);
                } else {
                    loginCallback.onError(GigyaError.generalError());
                }
            }

            @Override
            public void onError(GigyaError error) {
                loginCallback.onError(error);
            }
        });
    }


}
