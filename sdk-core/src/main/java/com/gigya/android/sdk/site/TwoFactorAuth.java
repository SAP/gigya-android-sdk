package com.gigya.android.sdk.site;

public class TwoFactorAuth {
    public Provider[] providers = new Provider[]{new Provider("gigyaPhone", true)};
    public EmailTemplate emailProvider = new EmailTemplate();
    public SMSProvider smsProvider = new SMSProvider();
}
