package com.gigya.android.sdk.site;

import java.util.HashMap;
import java.util.Map;

public class PreferencesCenter {

    public String defaultLanguage = "en";
    public Map<String, String> emailTemplates = new HashMap<>();
    public String RedirectURL;
    public String linkPlaceHolder = "$link";
}
