package com.gigya.android.sdk.account;

import com.gigya.android.sdk.account.models.GigyaAccount;
import com.google.gson.Gson;

import java.util.Map;

public interface IAccountService<A extends GigyaAccount> {

    void setAccountScheme(Class<A> scheme);

    Class<A> getAccountSchema();

    void setAccount(String json);

    void invalidateAccount();

    A getAccount();

    boolean isCachedAccount(String include, String profileExtraFields);

    void nextAccountInvalidationTimestamp();

    long getNextInvalidationTimestamp();

    void setAccountOverrideCache(boolean accountOverrideCache);

    void updateExtendedParametersRequest(String include, String profileExtraFields);

    Map<String, Object> calculateDiff(A cachedAccount, A updatedAccount);
}
