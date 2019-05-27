package com.gigya.android.sdk.account.models;

import android.support.annotation.Nullable;

public class Patent {

    @Nullable
    private String date;
    @Nullable
    private String number;
    @Nullable
    private String office;
    @Nullable
    private String status;
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
    public String getNumber() {
        return number;
    }

    public void setNumber(@Nullable String number) {
        this.number = number;
    }

    @Nullable
    public String getOffice() {
        return office;
    }

    public void setOffice(@Nullable String office) {
        this.office = office;
    }

    @Nullable
    public String getStatus() {
        return status;
    }

    public void setStatus(@Nullable String status) {
        this.status = status;
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
