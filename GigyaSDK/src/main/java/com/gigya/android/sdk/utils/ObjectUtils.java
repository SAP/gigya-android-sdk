package com.gigya.android.sdk.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ObjectUtils {

    public static <T> T firstNonNull(@Nullable T first, @NonNull T second) {
        return first != null ? first : second;
    }

    public static <T, V> boolean safeEquals(T first, V second) {
        if (first != null && second != null) {
            return first.equals(second);
        }
        return false;
    }

    public static Map<String, Object> objectDifference(Map<String, Object> original, Map<String, Object> updated) {
        Map<String, Object> result = new HashMap<>();
        if (original == null || updated == null) {
            return result;
        }
        Set<Map.Entry<String, Object>> filter = original.entrySet();
        for (Map.Entry<String, Object> entry : updated.entrySet()) {
            if (!filter.contains(entry)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        for (Map.Entry<String, Object> item : result.entrySet()) {
            final String key = item.getKey();
            final Object value = item.getValue();
            if (value instanceof Map) {
                if (updated.get(key) != null && original.get(key) != null) {
                    Map<String, Object> childResult = objectDifference((Map<String, Object>) original.get(key), (Map<String, Object>) updated.get(key));
                    result.put(key, childResult);

                }
            }
        }
        return result;
    }

    public static List<String> mergeRemovingDuplicates(List<String> first, List<String> second) {
        Set<String> set = new HashSet<>(first);
        set.addAll(second);
        return new ArrayList<>(set);
    }
}
