package com.gigya.android.sdk;

import java.util.Map;

public abstract class GigyaPluginCallback<T> extends GigyaCallback<T> {

    public abstract void onEvent(String eventName, Map<String, Object> parameters);

    public abstract void onCancel();
}
