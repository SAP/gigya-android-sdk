package com.gigya.android.sdk.services;

import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.utils.ObjectUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AccountService<A extends GigyaAccount> {

    /*
    Cached generic account object.
     */
    private A _cachedAccount;

    public A getAccount() {
        return _cachedAccount;
    }

    public void setAccount(A  account) {
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
    private Class<A> _accountScheme = (Class<A>) GigyaAccount.class;

    public Class<A> getAccountScheme() {
        return _accountScheme;
    }

    public void updateAccountScheme(Class<A> accountScheme) {
        _accountScheme = accountScheme;
    }

    //endregion

    //region ACCOUNT CACHING LOGIC

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
        if (!_accountOverrideCache && _accountCacheTime > 0) {
            _accountInvalidationTimestamp = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(_accountCacheTime);
        }
    }

    //endregion

    /**
     * Get account object objectDifference.
     */
    @SuppressWarnings({"ConstantConditions"})
    public Map<String, Object> calculateDiff(Gson gson, A cachedAccount, A updatedAccount) {
        /* Map updated account object to JSON -> Map. */
        final String updatedJson = gson.toJson(updatedAccount);
        Map<String, Object> updatedMap = gson.fromJson(updatedJson, new TypeToken<HashMap<String, Object>>() {
        }.getType());

        /* Map original account object to JSON -> Map. */
        final String originalJson = gson.toJson(cachedAccount);
        Map<String, Object> originalMap = gson.fromJson(originalJson, new TypeToken<HashMap<String, Object>>() {
        }.getType());

        /* Calculate objectDifference. */
        Map<String, Object> diff = ObjectUtils.objectDifference(originalMap, updatedMap);
        /* Must have UID or regToken. */
        if (updatedMap.containsKey("UID")) {
            diff.put("UID", updatedMap.get("UID"));
        } else if (updatedMap.containsKey("regToken")) {
            diff.put("regToken", updatedMap.get("regToken"));
        }
        serializeObjectFields(gson, diff);
        return diff;
    }

    /**
     * Object represented field values must be set as JSON Objects. So we are reverting each one to
     * its JSON String representation.
     */
    private void serializeObjectFields(Gson gson, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof Map) {
                map.put(entry.getKey(), gson.toJson(entry.getValue()));
            }
        }
    }
}
