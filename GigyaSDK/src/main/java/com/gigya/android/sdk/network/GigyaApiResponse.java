package com.gigya.android.sdk.network;

import android.support.annotation.Nullable;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.utils.ObjectUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Gigya generic response class.
 */
public class GigyaApiResponse {

    private static final String LOG_TAG = "GigyaApiResponse";

    public static final int INVALID_VALUE = -1;
    public static final int OK = 200;

    private String json;
    private Map<String, Object> mapped;

    // GSON Support.
    private Gson gson = new GsonBuilder().create();

    public Gson getGson() {
        return gson;
    }

    public GigyaApiResponse(String json) {
        this.json = json;
        try {
            JSONObject jo = new JSONObject(json);
            mapped = ObjectUtils.toMap(jo);
            GigyaLogger.debug(LOG_TAG, "json mapped!");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get JSON formatted of the current response.
     *
     * @return JSON formatted String.
     */
    public String asJson() {
        return this.json;
    }

    /**
     * Optional parsing of the current response with generic given type. Uses GSON as the parsing engine.
     *
     * @param clazz Requested type for parsing.
     * @return Parsed hard copy class according to provided type.
     */
    @Nullable
    public <T> T parseTo(Class<T> clazz) {
        try {
            return getGson().fromJson(asJson(), clazz);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Check if response parameters contains a specific key.
     *
     * @param key Required key for evaluation.
     * @return TRUE of key is present.
     */
    @SuppressWarnings("LoopStatementThatDoesntLoop")
    public boolean contains(String key) {
        return mapped.containsKey(key);
    }

    /**
     * Check if response parameters contains a specific nested key.
     * Nested key example: profile.firstName
     *
     * @param key Required key for evaluation.
     * @return TRUE of nested key is present.
     */
    public boolean containsNested(String key) {
        String[] split = key.split("\\.");
        if (split.length == 1) {
            return mapped.containsKey(key);
        } else {
            Map map = mapped;
            for (int i = 0; i < split.length; i++) {
                Object obj = map.get(split[i]);
                if (obj == null) {
                    return false;
                }
                if (obj instanceof Map) {
                    map = (Map) obj;
                } else if (i < split.length - 1) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Type field optional getter.
     * Allows fetching an parsed object from response parameters given required type.
     * Uses GSON as the parsing engine.
     *
     * @param key   Requested parameter key.
     * @param clazz Required  parsed object type.
     * @return Parsed hard copy class according to provided key and type.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getField(String key, Class<T> clazz) {
        String[] split = key.split("\\.");
        if (split.length == 1) {
            if (mapped.containsKey(key)) {
                final String json = gson.toJson(mapped.get(key));
                return gson.fromJson(json, clazz);
            }
        }
        if (containsNested(key)) {
            Map map = mapped;
            Object obj = null;
            for (int i = 0; i < split.length; i++) {
                obj = map.get(split[i]);
                if (i < split.length - 1) {
                    map = (Map) obj;
                }
            }
            if (obj == null) {
                return null;
            }
            if (obj.getClass() == clazz) {
                return (T) obj;
            }
        } else {
            return null;
        }
        return null;
    }

    //region ROOT ELEMENT GETTERS

    /**
     * Get response status code
     *
     * @return Integer status code.
     */
    public int getStatusCode() {
        try {
            return (int) mapped.get("statusCode");
        } catch (Exception ex) {
            ex.printStackTrace();
            return INVALID_VALUE;
        }
    }

    /**
     * Get response error code.
     *
     * @return Integer error code.
     */
    public int getErrorCode() {
        try {
            return (int) mapped.get("errorCode");
        } catch (Exception ex) {
            ex.printStackTrace();
            return INVALID_VALUE;
        }
    }

    /**
     * Get response error details if exists.
     *
     * @return String error details.
     */
    @Nullable
    public String getErrorDetails() {
        if (mapped.containsKey("errorDetails")) {
            return (String) mapped.get("errorDetails");
        }
        return null;
    }

    /**
     * Get response status reason if exists.
     *
     * @return String status reason.
     */
    @Nullable
    public String getStatusReason() {
        if (mapped.containsKey("statusReason")) {
            return (String) mapped.get("statusReason");
        }
        return null;
    }

    /**
     * Get response callId.
     *
     * @return String callId.
     */
    @Nullable
    public String getCallId() {
        if (mapped.containsKey("callId")) {
            return (String) mapped.get("callId");
        }
        return null;
    }

    /**
     * Get response time.
     *
     * @return String formatted timestamp.
     */
    @Nullable
    public String getTime() {
        if (mapped.containsKey("time")) {
            return (String) mapped.get("time");
        }
        return null;
    }

    //endregion
}
