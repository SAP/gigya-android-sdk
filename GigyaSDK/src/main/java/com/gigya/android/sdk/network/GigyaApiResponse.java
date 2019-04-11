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

    public String asJson() {
        return this.json;
    }

    @Nullable
    public <T> T parseTo(Class<T> clazz) {
        try {
            return getGson().fromJson(asJson(), clazz);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("LoopStatementThatDoesntLoop")
    public boolean contains(String key) {
        return mapped.containsKey(key);
    }

    public boolean containsNested(String key) {
        String[] split = key.split("\\.");
        if (split.length == 1) {
            return mapped.containsKey(key);
        } else {
            Map map = mapped;
            for (int i = 0; i < split.length - 1; i++) {
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

    public int getStatusCode() {
        try {
            return (int) mapped.get("statusCode");
        } catch (Exception ex) {
            ex.printStackTrace();
            return INVALID_VALUE;
        }
    }

    public int getErrorCode() {
        try {
            return (int) mapped.get("errorCode");
        } catch (Exception ex) {
            ex.printStackTrace();
            return INVALID_VALUE;
        }
    }

    @Nullable
    public String getErrorDetails() {
        if (mapped.containsKey("errorDetails")) {
            return (String) mapped.get("errorDetails");
        }
        return null;
    }

    @Nullable
    public String getStatusReason() {
        if (mapped.containsKey("statusReason")) {
            return (String) mapped.get("statusReason");
        }
        return null;
    }

    @Nullable
    public String getCallId() {
        if (mapped.containsKey("callId")) {
            return (String) mapped.get("callId");
        }
        return null;
    }

    @Nullable
    public String getTime() {
        if (mapped.containsKey("time")) {
            return (String) mapped.get("time");
        }
        return null;
    }

    //endregion
}
