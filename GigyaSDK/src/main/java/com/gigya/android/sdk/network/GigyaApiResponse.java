package com.gigya.android.sdk.network;

import android.support.annotation.Nullable;

import com.gigya.android.sdk.gson.PostProcessableTypeAdapterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;


/**
 * Gigya generic response class.
 */
public class GigyaApiResponse {

    public static final int INVALID_VALUE = -1;
    public static final int OK = 200;

    private String json;
    private LinkedTreeMap<String, Object> mapped;

    // GSON Support.
    private Gson gson = new GsonBuilder().registerTypeAdapterFactory(new PostProcessableTypeAdapterFactory()).create();

    public Gson getGson() {
        return gson;
    }

    public GigyaApiResponse(String json) {
        this.json = json;
        mapped = gson.fromJson(json, new TypeToken<LinkedTreeMap<String, Object>>() {
        }.getType());
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
            LinkedTreeMap map = mapped;
            for (int i = 0; i < split.length - 1; i++) {
                Object obj = map.get(split[i]);
                if (obj == null) {
                    return false;
                }
                if (obj instanceof LinkedTreeMap) {
                    map = (LinkedTreeMap) obj;
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
            LinkedTreeMap map = mapped;
            Object obj = null;
            for (int i = 0; i < split.length; i++) {
                obj = map.get(split[i]);
                if (i < split.length - 1) {
                    map = (LinkedTreeMap) obj;
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
    
    //region Root element getters

    public int getStatusCode() {
        try {
            final Double val = (Double) mapped.get("statusCode");
            return val.intValue();
        } catch (Exception ex) {
            ex.printStackTrace();
            return INVALID_VALUE;
        }
    }

    public int getErrorCode() {
        try {
            final Double val = (Double) mapped.get("errorCode");
            return val.intValue();
        } catch (Exception ex) {
            ex.printStackTrace();
            return INVALID_VALUE;
        }
    }

    @Nullable
    public String getErrorDetails() {
        return (String) mapped.get("errorDetails");
    }

    @Nullable
    public String getStatusReason() {
        return (String) mapped.get("statusReason");
    }

    @Nullable
    public String getCallId() {
        return (String) mapped.get("callId");
    }

    @Nullable
    public String getTime() {
        return (String) mapped.get("time");
    }

    //endregion

}
