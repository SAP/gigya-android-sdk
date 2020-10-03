package com.gigya.android.sdk.account.models;

import androidx.annotation.Nullable;

public class Work {

    @Nullable
    private String company;
    @Nullable
    private String companyID;
    @Nullable
    private Long companySize;
    @Nullable
    private String description;
    @Nullable
    private String endDate;
    @Nullable
    private String industry;
    private boolean isCurrent;
    @Nullable
    private String location;
    @Nullable
    private String startDate;
    @Nullable
    private String title;

    @Nullable
    public String getCompany() {
        return company;
    }

    public void setCompany(@Nullable String company) {
        this.company = company;
    }

    @Nullable
    public String getCompanyID() {
        return companyID;
    }

    public void setCompanyID(@Nullable String companyID) {
        this.companyID = companyID;
    }

    @Nullable
    public Long getCompanySize() {
        return companySize;
    }

    public void setCompanySize(@Nullable Long companySize) {
        this.companySize = companySize;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @Nullable
    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(@Nullable String endDate) {
        this.endDate = endDate;
    }

    @Nullable
    public String getIndustry() {
        return industry;
    }

    public void setIndustry(@Nullable String industry) {
        this.industry = industry;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public void setCurrent(boolean current) {
        isCurrent = current;
    }

    @Nullable
    public String getLocation() {
        return location;
    }

    public void setLocation(@Nullable String location) {
        this.location = location;
    }

    @Nullable
    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(@Nullable String startDate) {
        this.startDate = startDate;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    public void setTitle(@Nullable String title) {
        this.title = title;
    }
}
