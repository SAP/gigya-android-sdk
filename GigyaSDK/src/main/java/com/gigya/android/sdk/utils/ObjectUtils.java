package com.gigya.android.sdk.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ObjectUtils {

    public static Map<String, Object> difference(Map<String, Object> original, Map<String, Object> updated) {
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
                    Map<String, Object> childResult = difference((Map<String, Object>) original.get(key), (Map<String, Object>) updated.get(key));
                    result.put(key, childResult);
                }
            }
        }

        return result;
    }
}
