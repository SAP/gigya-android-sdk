package com.gigya.android.sdk.interruption;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.api.GetConflictingAccountApi;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaResponse;

public class LoginIdentifierExistsResolver extends LoginResolver {

    public LoginIdentifierExistsResolver(GigyaLoginCallback loginCallback) {
        super(loginCallback);
    }

    public void resolve(final String regToken) {
        apiManager.getConflictingAccounts(regToken, new GigyaCallback<GigyaResponse>() {
            @Override
            public void onSuccess(GigyaResponse obj) {
                final GetConflictingAccountApi.ConflictingAccount ca = obj.getField("conflictingAccount",
                        GetConflictingAccountApi.ConflictingAccount.class);
                if (ca == null) {
                    loginCallback.onError(GigyaError.generalError());
                    return;
                }
                loginCallback.onConflictingAccounts(ca.getLoginProviders(), new ConflictingProviderResolver(
                        loginCallback, regToken
                ));
            }

            @Override
            public void onError(GigyaError error) {
                loginCallback.onError(error);
            }
        });
    }
}
