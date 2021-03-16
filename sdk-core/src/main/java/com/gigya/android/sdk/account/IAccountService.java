package com.gigya.android.sdk.account;

import com.gigya.android.sdk.account.models.GigyaAccount;

import java.util.Map;

public interface IAccountService<A extends GigyaAccount> {

    void setAccountScheme(Class<A> scheme);

    Class<A> getAccountSchema();

    void setAccount(String json);

    void invalidateAccount();

    A getAccount();

    boolean isCachedAccount();

    void nextAccountInvalidationTimestamp();

    long getNextInvalidationTimestamp();

    void setAccountOverrideCache(boolean accountOverrideCache);

    Map<String, Object> calculateDiff(A cachedAccount, A updatedAccount);
}
