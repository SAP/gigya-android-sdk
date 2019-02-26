package com.gigya.android.sdk.api;

import com.gigya.android.sdk.AccountManager;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.interruption.LinkedAccountResolver;
import com.gigya.android.sdk.interruption.tfa.TFAResolver;
import com.gigya.android.sdk.model.GigyaAccount;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

public class InterruptionEnabledApi<T extends GigyaAccount> extends BaseApi<T> {

    protected final AccountManager accountManager;

    public InterruptionEnabledApi(NetworkAdapter networkAdapter, SessionManager sessionManager, AccountManager accountManager) {
        super(networkAdapter, sessionManager);
        this.accountManager = accountManager;
    }

    protected void handleInterruptionError(GigyaResponse response, final GigyaLoginCallback<T> loginCallback) {
        if (!interrupted(response, loginCallback)) {
            /* Interruption is not handled. Forward the error. */
            loginCallback.forwardError(response);
        }
    }

    private boolean interrupted(GigyaResponse response, final GigyaLoginCallback<T> loginCallback) {
        if (sessionManager.getConfiguration().isInterruptionsEnabled()) {
            /* Get regToken from parameter map. */
            final String regToken = response.getField("regToken", String.class);
            final int errorCode = response.getErrorCode();
            switch (errorCode) {
                case GigyaError.Codes.ERROR_ACCOUNT_PENDING_VERIFICATION:
                    loginCallback.onPendingVerification(response, regToken);
                    return true;
                case GigyaError.Codes.ERROR_ACCOUNT_PENDING_REGISTRATION:
                    loginCallback.onPendingRegistration(response, regToken);
                    return true;
                case GigyaError.Codes.ERROR_PENDING_PASSWORD_CHANGE:
                    loginCallback.onPendingPasswordChange(response);
                    return true;
                case GigyaError.Codes.ERROR_LOGIN_IDENTIFIER_EXISTS:
                    //TODO refactor
                    return true;
                case GigyaError.Codes.ERROR_PENDING_TWO_FACTOR_REGISTRATION:
                case GigyaError.Codes.ERROR_PENDING_TWO_FACTOR_VERIFICATION:
                    TFAResolver resolver = new TFAResolver<>(networkAdapter, sessionManager, accountManager,
                            loginCallback);
                    resolver.setRegToken(regToken);
                    resolver.init();
                    return true;
            }
        }
        return false;
    }

    /* Evaluating responses that are tagged as success but still require error handling. */
    protected boolean evaluateSuccessError(GigyaResponse response, final GigyaLoginCallback loginCallback) {
        if (!sessionManager.getConfiguration().isInterruptionsEnabled()) {
            return false;
        }
        final int errorCode = response.getErrorCode();
        if (errorCode == 0) {
            return false;
        }
        switch (errorCode) {
            case GigyaError.Codes.SUCCESS_ERROR_ACCOUNT_LINKED:
                final String regToken = response.getField("regToken", String.class);
                new LinkedAccountResolver(response, loginCallback).resolve(regToken);
                return true;
        }
        return false;
    }
}
