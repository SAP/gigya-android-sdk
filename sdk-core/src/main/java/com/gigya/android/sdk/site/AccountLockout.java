package com.gigya.android.sdk.site;

public class AccountLockout {

    public int failedLoginThreshold = 0;
    public int lockoutTimeSec = 5 * 60;
    public int failedLoginResetSec = 0;
}
