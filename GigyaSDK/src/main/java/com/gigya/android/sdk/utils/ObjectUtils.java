package com.gigya.android.sdk.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

    public static <T,V> boolean nullAllowedEquals(T first, V second) {
        if (first != null && second != null) {
            return first.equals(second);
        }
        if (first == null && second == null) {
            return true;
        }
        return false;
    }

    public static Map<String, Object> toMap(JSONObject object) throws JSONException {
        Map<String, Object> map = new HashMap<>();

        Iterator<String> keysItr = object.keys();
        while (keysItr.hasNext()) {
            String key = keysItr.next();
            Object value = object.get(key);

            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    }

    private static List<Object> toList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }

    public static Map<String, Object> objectDifference(Map<String, Object> original, Map<String, Object> updated) {
        Map<String, Object> result = new HashMap<>();
        if (original == null || updated == null) {
            return result;
        }

        // Check removed items.

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

    public static <T> T deepCopy(Gson gson, T obj, Class<T> clazz) {
        final String json = gson.toJson(obj);
        return gson.fromJson(json, clazz);
    }

    public static String commaConcat(String[] input) {
        if (input.length > 0) {
            StringBuilder concat = new StringBuilder();
            for (String string : input) {
                concat.append(string).append(",");
            }

            concat.deleteCharAt(concat.length() - 1);
            return concat.toString();
        }
        return "";
    }
}
