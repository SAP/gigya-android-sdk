package com.gigya.android.sdk.interruption;

import androidx.annotation.NonNull;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.RestAdapter;

import java.util.Map;

/**
 * Helper class used to resolve flows that get interrupted with error "Account Pending Registration" (206001).
 * Missing registration field will appear in the interruption "errorDetails" field.
 * In order to resolve the flow you will need to set the missing account fields using the setAccount method.
 *
 * @param <A> Account scheme.
 */
public class PendingRegistrationResolver<A extends GigyaAccount> extends Resolver<A> implements IPendingRegistrationResolver {

    public PendingRegistrationResolver(GigyaLoginCallback<A> loginCallback,
                                       GigyaApiResponse interruption,
                                       IBusinessApiService<A> businessApiService) {
        super(loginCallback, interruption, businessApiService);
    }

    @Override
    public void setAccount(@NonNull Map<String, Object> missingAccountFields) {
        // Reg token field is mandatory in order to resolve this issue.
        missingAccountFields.put("regToken", getRegToken());
        // Using "send" method in order to avoid flow constraints in the regular "setAccount" BA.
        _businessApiService.send(
                GigyaDefinitions.API.API_SET_ACCOUNT_INFO,
                missingAccountFields,
                RestAdapter.POST,
                GigyaApiResponse.class,
                new GigyaCallback<GigyaApiResponse>() {
                    @Override
                    public void onSuccess(GigyaApiResponse obj) {
                        finalizeRegistration(null);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        _loginCallback.onError(error);
                    }
                });
    }
}
