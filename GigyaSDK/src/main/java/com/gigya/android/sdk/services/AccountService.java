package com.gigya.android.sdk.services;

import com.gigya.android.sdk.model.account.GigyaAccount;

import java.util.concurrent.TimeUnit;

public class AccountService<T extends GigyaAccount> {

    /*
    Cached generic account object.
     */
    private T _cachedAccount;

    public T getAccount() {
        return _cachedAccount;
    }

    public void setAccount(T account) {
        _cachedAccount = account;
    }

    /*
     * Flush account data (nullify).
     */
    public void invalidateAccount() {
        _cachedAccount = null;
    }

    // Account scheme.

    @SuppressWarnings("unchecked")
    private Class<T> _accountScheme = (Class<T>) GigyaAccount.class;

    public void updateAccountSchene(Class<T> accountScheme) {
        _accountScheme = accountScheme;
    }

    //endregion

    //region Account caching logic.

    /*
    Invalidation timestamp for cached account.
     */
    private long _accountInvalidationTimestamp = 0L;
    /*
    Override account caching flag. Set to TRUE to override caching policy. This will result in consecutive getAccount HTTP requests firing
    every time the user will request "getAccount" from the SDK.
     */
    private boolean _accountOverrideCache = false;

    /*
    Account caching time parameter in minutes.
     */
    private int _accountCacheTime = 0;

    public void setAccountCacheTime(int accountCacheTime) {
        _accountCacheTime = accountCacheTime;
    }

    public void setAccountOverrideCache(boolean accountOverrideCache) {
        _accountOverrideCache = accountOverrideCache;
    }

    /**
     * Validate if the account is currently cached.
     * Caching of the account is only valid when caching policy is not overridden and the caching timestamp is valid.
     *
     * @return True if available cached account.
     */
    public boolean isCachedAccount() {
        return !_accountOverrideCache && _cachedAccount != null && System.currentTimeMillis() < _accountInvalidationTimestamp;
    }

    /**
     * update he next cached account invalidation timestamp.
     */
    public void nextAccountInvalidationTimestamp() {
        if (!_accountOverrideCache) {
            _accountInvalidationTimestamp = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(_accountCacheTime);
        }
    }

    //endregion
}
