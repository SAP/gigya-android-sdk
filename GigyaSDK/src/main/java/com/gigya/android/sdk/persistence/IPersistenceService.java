package com.gigya.android.sdk.persistence;

import com.gigya.android.sdk.GigyaDefinitions;

import java.util.Set;

public interface IPersistenceService {

    boolean contains(String key);

    String getString(String key, String defValue);

    Long getLong(String key, Long defValue);

    void add(String key, Object element);

    void remove(String... keys);

    Set<String> getSet(String key, Set<String> defValue);

    Set<String> getSocialProviders();

    void addSocialProvider(@GigyaDefinitions.Providers.SocialProvider String provider);
}
