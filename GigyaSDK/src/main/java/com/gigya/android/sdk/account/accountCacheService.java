package com.gigya.android.sdk.account;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.utils.ObjectUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Account handling service responsible for holding a cached copy of the current account instance.
 *
 * @param <A> Typed account instance (extends GigyaAccount).
 */
public class accountCacheService<A extends GigyaAccount> implements IAccountService<A> {

    final private Config _config;

    /*
    Cached generic account object.
     */
    private String _cachedAccount;

    @SuppressWarnings("unchecked")
    private Class<A> _accountScheme;

    /*
    Invalidation timestamp for cached account.
     */
    private long _accountInvalidationTimestamp = 0L;
    /*
    Override account caching flag. Set to TRUE to override caching policy. This will result in consecutive getAccount HTTP requests firing
    every time the user will request "getAccount" from the SDK.
     */
    private boolean _accountOverrideCache = false;
    public accountCacheService(Config config, GigyaAccountClass<A> accountClazz) {
        _config = config;
        _accountScheme = accountClazz.getAccountClass();
    }

    @Override
    public void setAccountScheme(Class<A> scheme) {
        _accountScheme = scheme;
    }

    @Override
    public Class<A> getAccountSchema() {
        return _accountScheme;
    }

    @Override
    public void setAccount(String json) {
        _cachedAccount = json;
        nextAccountInvalidationTimestamp();
    }

    @Override
    public void invalidateAccount() {
        _cachedAccount = null;
    }

    @Override
    public A getAccount() {
        return new Gson().fromJson(_cachedAccount, _accountScheme);
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

    //region ACCOUNT SPECIFIC LOGIC

    /**
     * Get account object objectDifference.
     */
    @SuppressWarnings({"ConstantConditions"})
    @Override
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
    public static void serializeObjectFields(Gson gson, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof Map) {
                map.put(entry.getKey(), gson.toJson(entry.getValue()));
            }
        }
    }

    //endregion
}
