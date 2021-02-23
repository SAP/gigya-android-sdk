package com.gigya.android.sdk.site;

import java.util.Map;

public class Provider {

    public Provider(String name, boolean enabled) {
        this.name = name;
        this.enabled = enabled;
    }

    public String name;
    public boolean enabled;
    public Map<String, String> params;
}
