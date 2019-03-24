package com.gigya.android.sdk.biometric.model;

public class GigyaPromptInfo {

    private String title;
    private String subtitle;
    private String description;

    public GigyaPromptInfo(String title, String subtitle, String description) {
        this.title = title;
        this.subtitle = subtitle;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getDescription() {
        return description;
    }
}
