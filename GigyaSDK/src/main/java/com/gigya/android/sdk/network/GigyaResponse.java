package com.gigya.android.sdk.network;

import android.support.annotation.Nullable;

import com.gigya.android.sdk.log.GigyaLogger;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * Gigya generic response class.
 */
public class GigyaResponse {

    private static final String LOG_TAG = "GigyaResponse";

    private JSONObject jsonObject;
    private Map<String, Object> values = new HashMap<>();

    public static final int INVALID_VALUE = -1;

    public GigyaResponse(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
        try {
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
        try {
            return jsonObject.toString(2);
        } catch (JSONException e) {
            e.printStackTrace();
            return jsonObject.toString();
        }
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
                field = new Gson().fromJson(field.toString(), clazz);
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
