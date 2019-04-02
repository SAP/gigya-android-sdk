package com.gigya.android.sdk.persistence;

import java.util.Set;

public interface IPersistenceService {

    boolean contains(String key);

    public String getString(String key, String defValue);

    public Long getLong(String key, Long defValue);

    public void add(String key, Object element);

    public void remove(String... keys);

    Set<String> getSet(String key, Set<String> defValue);
}
