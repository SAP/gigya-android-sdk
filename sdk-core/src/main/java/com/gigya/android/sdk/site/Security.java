package com.gigya.android.sdk.site;

public class Security {

    public AccountLockout accountLockout;
    public Captcha captcha;
    public IpLockout ipLockout;
    public int passwordChangeInterval = 0;
    public int passwordHistorySize = 0;
    public boolean riskAssessmentWithReCaptchaV3 = false;
}
