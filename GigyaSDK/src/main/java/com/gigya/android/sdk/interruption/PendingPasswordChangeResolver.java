package com.gigya.android.sdk.interruption;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class PendingPasswordChangeResolver extends LoginResolver {

    private String regToken;
    private String securityQuestion;

    public String getRegToken() {
        return regToken;
    }

    public String getSecurityQuestion() {
        return securityQuestion;
    }

    public PendingPasswordChangeResolver(GigyaResponse response, GigyaLoginCallback loginCallback, String regToken) {
        super(response, loginCallback);
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
                loginCallback.onResetPasswordEmailSent();
            }

            @Override
            public void onError(GigyaError error) {
                if (!securityValidationFailed(error)) {
                    loginCallback.onError(error);
                }
            }
        });
    }

    private boolean securityValidationFailed(GigyaError error) {
        final int errorCode = error.getErrorCode();
        if (errorCode == GigyaError.Codes.ERROR_SECURITY_VERIFICATION_FAILED) {
            /* Try to fetch the security question from the response. */
            JsonObject jsonObject = new Gson().fromJson(error.getData(), JsonObject.class);
            securityQuestion = jsonObject.get("secretQuestion").getAsString();
            if (securityQuestion != null) {
                loginCallback.onResetPasswordSecurityVerificationFailed(PendingPasswordChangeResolver.this);
                return true;
            }
        }
        return false;
    }

    /**
     * Resolve Pending password state change using loginID, newPassword, secretAnswer parameters.
     * Using this resolve requires your site setup to use "passwordReset.requireSecurityCheck: true" in your site policy.
     *
     * @param loginID      User loginID.
     * @param newPassword  User desired password.
     * @param secretAnswer User secret answer.
     * @see <a href=https://developers.gigya.com/display/GD/Using+Security+Questions></a>
     */
    public void resolve(final String loginID, final String newPassword, final String secretAnswer) {
        Map<String, Object> params = new HashMap<>();
        params.put("loginID", loginID);
        params.put("newPassword", newPassword);
        params.put("secretAnswer", secretAnswer);
        apiManager.resetPassword(params, new GigyaCallback<GigyaResponse>() {
            @Override
            public void onSuccess(GigyaResponse obj) {
                // Re-login.
                Map<String, Object> loginMap = new HashMap<>();
                loginMap.put("loginID", loginID);
                loginMap.put("password", newPassword);
                apiManager.login(loginMap, loginCallback);
            }

            @Override
            public void onError(GigyaError error) {
                loginCallback.onError(error);
            }
        });
    }


}
