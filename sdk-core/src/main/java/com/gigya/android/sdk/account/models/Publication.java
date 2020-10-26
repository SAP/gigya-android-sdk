package com.gigya.android.sdk.account.models;

import androidx.annotation.Nullable;

public class Publication {

    @Nullable
    private String date;
    @Nullable
    private String publisher;
    @Nullable
    private String summary;
    @Nullable
    private String title;
    @Nullable
    private String url;

    @Nullable
    public String getDate() {
        return date;
    }

    public void setDate(@Nullable String date) {
        this.date = date;
    }

    @Nullable
    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(@Nullable String publisher) {
        this.publisher = publisher;
    }

    @Nullable
    public String getSummary() {
        return summary;
    }

    public void setSummary(@Nullable String summary) {
        this.summary = summary;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    public void setTitle(@Nullable String title) {
        this.title = title;
    }

    @Nullable
    public String getUrl() {
        return url;
    }

    public void setUrl(@Nullable String url) {
        this.url = url;
    }
}
