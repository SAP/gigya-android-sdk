package com.gigya.android.sdk.network;

import android.support.annotation.Nullable;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.gson.PostProcessableTypeAdapterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * Gigya generic response class.
 */
public class GigyaApiResponse {

    private static final String LOG_TAG = "GigyaApiResponse";
    public static final int INVALID_VALUE = -1;
    public static final int OK = 200;

    private String json;
    private JSONObject jsonObject;
    private Map<String, Object> values = new HashMap<>();

    // GSON Support.
    private Gson gson = new GsonBuilder().registerTypeAdapterFactory(new PostProcessableTypeAdapterFactory()).create();

    public Gson getGson() {
        return gson;
    }

    public GigyaApiResponse(String json) {
        this.json = json;
        try {
            this.jsonObject = new JSONObject(json);
            flatMap(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
            GigyaLogger.error(LOG_TAG, "Failed to flatMap json object from response");
        }
    }

    /*
    Flatten json object for faster object fetch.
     */
    private void flatMap(JSONObject object) throws JSONException {
        Iterator<?> keys = object.keys();
        while (keys.hasNext()) {
            String key = keys.next().toString();
            Object value = object.get(key);
            values.put(key, value);
            if (value.getClass().equals(JSONObject.class)) {
                JSONObject nested = (JSONObject) value;
                flatMap(nested);
            }
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

    public boolean contains(String key) {
        return values.containsKey(key);
    }

    @Nullable
    public Object getField(String key) {
        if (!values.containsKey(key)) {
            return null;
        }
        return values.get(key);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getField(String key, Class<T> clazz) {
        try {
            if (!values.containsKey(key)) {
                return null;
            }
            Object field = values.get(key);
            if (field instanceof JSONObject) {
                field = gson.fromJson(field.toString(), clazz);
            }
            return clazz.isInstance(field) ? clazz.cast(field) : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //region Root element getters

    public int getStatusCode() {
        return jsonObject.optInt("statusCode", INVALID_VALUE);
    }

    public int getErrorCode() {
        return jsonObject.optInt("errorCode", INVALID_VALUE);
    }

    @Nullable
    public String getErrorDetails() {
        return jsonObject.optString("errorDetails");
    }

    @Nullable
    public String getStatusReason() {
        return jsonObject.optString("statusReason");
    }

    @Nullable
    public String getCallId() {
        return jsonObject.optString("callId");
    }

    @Nullable
    public String getTime() {
        return jsonObject.optString("time");
    }

    //endregion

}
