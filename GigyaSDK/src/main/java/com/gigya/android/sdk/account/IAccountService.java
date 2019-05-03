package com.gigya.android.sdk.account;

import com.gigya.android.sdk.model.account.GigyaAccount;
import com.google.gson.Gson;

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

    Map<String, Object> calculateDiff(Gson gson, A cachedAccount, A updatedAccount);
}
