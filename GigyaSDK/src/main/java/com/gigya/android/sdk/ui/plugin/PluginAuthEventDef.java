package com.gigya.android.sdk.ui.plugin;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class PluginAuthEventDef {

    @Retention(RetentionPolicy.SOURCE)

    @StringDef({LOGIN_STARTED, LOGIN, LOGOUT, ADD_CONNECTION, REMOVE_CONNECTION, CANCELED})

    public @interface PluginAuthEvent {
    }

    private @PluginAuthEvent
    String pluginAuthEvent;

    public static final String LOGIN_STARTED = "login_started";
    public static final String LOGIN = "login";
    public static final String LOGOUT = "logout";
    public static final String ADD_CONNECTION = "addConnection";
    public static final String REMOVE_CONNECTION = "removeConnection";
    public static final String CANCELED = "canceled";

    public PluginAuthEventDef(@PluginAuthEvent String pluginAuthEvent) {
        this.pluginAuthEvent = pluginAuthEvent;
    }
}
