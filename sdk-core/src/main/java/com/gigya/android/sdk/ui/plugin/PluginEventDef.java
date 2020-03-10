package com.gigya.android.sdk.ui.plugin;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class PluginEventDef {

    @Retention(RetentionPolicy.SOURCE)

    @StringDef({BEFORE_SCREEN_LOAD, LOAD, AFTER_SCREEN_LOAD, FIELD_CHANGED, BEFORE_VALIDATION,
            AFTER_VALIDATION, BEFORE_SUBMIT, SUBMIT, AFTER_SUBMIT, ERROR, HIDE})

    public @interface PluginEvent {
    }

    private @PluginEvent
    String pluginEvent;

    public static final String BEFORE_SCREEN_LOAD = "beforeScreenLoad";
    public static final String LOAD = "load";
    public static final String AFTER_SCREEN_LOAD = "afterScreenLoad";
    public static final String FIELD_CHANGED = "fieldChanged";
    public static final String BEFORE_VALIDATION = "beforeValidation";
    public static final String AFTER_VALIDATION = "afterValidation";
    public static final String BEFORE_SUBMIT = "beforeSubmit";
    public static final String SUBMIT = "submit";
    public static final String AFTER_SUBMIT = "afterSubmit";
    public static final String ERROR = "error";
    public static final String HIDE = "hide";

    public PluginEventDef(@PluginEvent String pluginEvent) {
        this.pluginEvent = pluginEvent;
    }
}
