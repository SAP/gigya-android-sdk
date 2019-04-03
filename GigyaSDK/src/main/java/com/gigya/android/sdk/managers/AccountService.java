package com.gigya.android.sdk.managers;

import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.services.Config;
import com.google.gson.Gson;

import java.util.concurrent.TimeUnit;

public class AccountService<A extends GigyaAccount> implements IAccountService<A> {

    final private Config _config;

    public AccountService(Config config) {
        _config = config;
    }

    /*
    Cached generic account object.
     */
    private A _cachedAccount;

    @SuppressWarnings("unchecked")
    private Class<A> _accountScheme = (Class<A>) GigyaAccount.class;

    /*
    Invalidation timestamp for cached account.
     */
    private long _accountInvalidationTimestamp = 0L;
    /*
    Override account caching flag. Set to TRUE to override caching policy. This will result in consecutive getAccount HTTP requests firing
    every time the user will request "getAccount" from the SDK.
     */
    private boolean _accountOverrideCache = false;

    @Override
    public void setAccountScheme(Class<A> scheme) {
        _accountScheme = scheme;
    }

    @Override
    public Class<A> getAccountScheme() {
        return _accountScheme;
    }

    @Override
    public void setAccount(String json) {
        _cachedAccount = new Gson().fromJson(json, _accountScheme);
        nextAccountInvalidationTimestamp();
    }

    @Override
    public void invalidateAccount() {
        _cachedAccount = null;
    }

    @Override
    public A getAccount() {
        return _cachedAccount;
    }

    @Override
    public boolean isCachedAccount() {
        return !_accountOverrideCache && _cachedAccount != null && System.currentTimeMillis() < _accountInvalidationTimestamp;
    }

    @Override
    public void nextAccountInvalidationTimestamp() {
        if (_cachedAccount == null) {
            return;
        }
        final int accountCacheTime = _config.getAccountCacheTime();
        if (!_accountOverrideCache && accountCacheTime > 0) {
            _accountInvalidationTimestamp = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(accountCacheTime);
        }
    }

    @Override
    public long getNextInvalidationTimestamp() {
        return _accountInvalidationTimestamp;
    }

    @Override
    public void setAccountOverrideCache(boolean accountOverrideCache) {
        _accountOverrideCache = accountOverrideCache;
    }
}
