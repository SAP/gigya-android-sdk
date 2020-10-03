package com.gigya.android.sdk.account.models;

import androidx.annotation.Nullable;

public class Certification {

    @Nullable
    private String authority;
    @Nullable
    private String endDate;
    @Nullable
    private String name;
    @Nullable
    private String number;
    @Nullable
    private String startDate;

    @Nullable
    public String getAuthority() {
        return authority;
    }

    public void setAuthority(@Nullable String authority) {
        this.authority = authority;
    }

    @Nullable
    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(@Nullable String endDate) {
        this.endDate = endDate;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    @Nullable
    public String getNumber() {
        return number;
    }

    public void setNumber(@Nullable String number) {
        this.number = number;
    }

    @Nullable
    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(@Nullable String startDate) {
        this.startDate = startDate;
    }
}
