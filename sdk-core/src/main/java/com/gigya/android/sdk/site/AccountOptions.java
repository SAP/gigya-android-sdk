package com.gigya.android.sdk.site;

enum LoginIdentifierConflict {
    failOnSiteConflictingIdentity, ignore, failOnAnyConflictingIdentity
}

enum LoginIdentifiers {
    email, username, providerEmail
}

public class AccountOptions {

    public boolean verifyEmail = false;
    public boolean verifyProviderEmail = false;
    public boolean useCodeVerification = false;
    public boolean allowUnverifiedLogin = false;
    public boolean preventLoginIDHarvesting = false;
    public boolean sendWelcomeEmail = false;
    public boolean sendAccountDeletedEmail = false;
    public String defaultLanguage = "en";
    public LoginIdentifiers loginIdentifiers = LoginIdentifiers.email;
    public LoginIdentifierConflict loginIdentifierConflict = LoginIdentifierConflict.failOnAnyConflictingIdentity;
}
