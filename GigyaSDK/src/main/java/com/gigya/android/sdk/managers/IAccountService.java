package com.gigya.android.sdk.managers;

import com.gigya.android.sdk.model.account.GigyaAccount;

public interface IAccountService<A extends GigyaAccount> {

    void setAccountScheme(Class<A> scheme);

    Class<A> getAccountScheme();

    void setAccount(String json);

    void invalidateAccount();

    A getAccount();

    boolean isCachedAccount();

    void nextAccountInvalidationTimestamp();

    long getNextInvalidationTimestamp();

    void setAccountOverrideCache(boolean accountOverrideCache);

}
