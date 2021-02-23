package com.gigya.android.sdk.account;

public class AccountConfig {

    private int cacheTime = 0;
    private String[] include;
    private String[] extraProfileFields;

    public int getCacheTime() {
        return cacheTime;
    }

    public void setCacheTime(int cacheTime) {
        this.cacheTime = cacheTime;
    }

    public String[] getInclude() {
        return include;
    }

    public void setInclude(String[] include) {
        this.include = include;
    }

    public String[] getExtraProfileFields() {
        return extraProfileFields;
    }

    public void setExtraProfileFields(String[] extraProfileFields) {
        this.extraProfileFields = extraProfileFields;
    }
}
