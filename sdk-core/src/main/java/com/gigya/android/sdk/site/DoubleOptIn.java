package com.gigya.android.sdk.site;

import java.util.Map;

public class DoubleOptIn {

    public String defaultLanguage = "en";
    public Map<String, String> confirmationEmailTemplates;
    public String nextURL;
    public double confirmationLinkExpiration = 1209600;
}
