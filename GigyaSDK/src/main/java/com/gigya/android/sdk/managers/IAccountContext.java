package com.gigya.android.sdk.managers;

import com.gigya.android.sdk.model.account.GigyaAccount;

public interface IAccountContext<A extends GigyaAccount> {

    Class<A> getAccountScheme();

    A getCachedAccount();

    boolean isCachedAccount();

    void setAccount(A account);
}
