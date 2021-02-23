package com.gigya.android.sdk.site;

import java.util.List;
import java.util.Map;

public class PasswordReset {

    public String defaultLanguage = "en";
    public Map<String, String> emailTemplates;
    public boolean requireSecurityCheck = false;
    public List<String> securityFields;
    public String resetURL;
    public int tokenExpiration = 60 * 60;
    public boolean sendConfirmationEmail = false;

}
