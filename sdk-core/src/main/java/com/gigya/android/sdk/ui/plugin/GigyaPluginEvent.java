package com.gigya.android.sdk.ui.plugin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.util.Map;

public class GigyaPluginEvent {

    @NonNull
    final private Map<String, Object> eventMap;

    public GigyaPluginEvent(@NonNull Map<String, Object> eventMap) {
        this.eventMap = eventMap;
    }

    @Nullable
    @PluginEventDef.PluginEvent
    public String getEvent() {
        final String eventName = (String) eventMap.get("eventName");
        if (eventName == null) {
            return null;
        }
        return eventName;
    }

    public String asJson() {
        return new JSONObject(eventMap).toString();
    }

    @NonNull
    public Map<String, Object> getEventMap() {
        return this.eventMap;
    }

}
