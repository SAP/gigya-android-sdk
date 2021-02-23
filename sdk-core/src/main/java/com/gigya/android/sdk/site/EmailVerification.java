package com.gigya.android.sdk.site;

import java.util.List;
import java.util.Map;

public class EmailVerification {

    public String defaultLanguage = "en";
    public Map<String, String> emailTemplates;
    public String nextURL;
    public int verificationEmailExpiration = 60 * 60 * 24;
    public boolean AutoLogin = false;
    public List<NextURLMapping> nextURLMapping;

}
