package com.gigya.android.sdk;

import android.support.annotation.NonNull;

import com.gigya.android.sdk.model.GigyaAccount;

import java.util.concurrent.TimeUnit;

public class AccountManager<T> {

    /*
     * Account object reference (cached).
     */
    private T _account;

    @SuppressWarnings("unchecked")
    @NonNull
    private Class<T> _accountClazz = (Class<T>) GigyaAccount.class;

    private long _accountInvalidationTimestamp = 0L;
    private boolean _accountOverrideCache = false;
    private int _accountCacheTime = 0;

    public void setAccountCacheTime(int accountCacheTime) {
        _accountCacheTime = accountCacheTime;
    }

    public void setAccountOverrideCache(boolean accountOverrideCache) {
        _accountOverrideCache = accountOverrideCache;
    }

    /*
     * Flush account data (nullify).
     */
    public void invalidateAccount() {
        _account = null;
    }

    public T getAccount() {
        return _account;
    }

    public void setAccount(T account) {
        _account = account;
    }

    public void setAccountClazz(Class<T> accountClazz) {
        _accountClazz = accountClazz;
    }

    public Class<T> getAccountClazz() {
        return _accountClazz;
    }

    public boolean getCachedAccount() {
        return !_accountOverrideCache && _account != null && System.currentTimeMillis() < _accountInvalidationTimestamp;
    }

    public void nextAccountInvalidationTimestamp() {
        if (!_accountOverrideCache) {
            _accountInvalidationTimestamp = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(_accountCacheTime);
        }
    }
}
