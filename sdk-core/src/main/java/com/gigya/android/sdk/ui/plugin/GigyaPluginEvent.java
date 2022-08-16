package com.gigya.android.sdk.ui.plugin;

import static com.gigya.android.sdk.ui.plugin.PluginEventDef.HIDE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class GigyaPluginEvent {

    public static final String EVENT_NAME = "eventName";
    public static final String FINALIZED = "isFlowFinalized";

    @NonNull
    final private Map<String, Object> eventMap;

    public GigyaPluginEvent(@NonNull Map<String, Object> eventMap) {
        this.eventMap = eventMap;
    }

    @Nullable
    @PluginEventDef.PluginEvent
    public String getEvent() {
        final String eventName = (String) eventMap.get(EVENT_NAME);
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

    public static GigyaPluginEvent hide() {
        Map<String, Object> eventMap = new HashMap<>(1);
        eventMap.put(EVENT_NAME, HIDE);
        eventMap.put(FINALIZED, true);
        return new GigyaPluginEvent(eventMap);
    }


}
