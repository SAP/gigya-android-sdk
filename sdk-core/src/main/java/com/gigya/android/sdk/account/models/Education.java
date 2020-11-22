package com.gigya.android.sdk.account.models;

import androidx.annotation.Nullable;

public class Education {

    @Nullable
    private String degree;
    @Nullable
    private String endYear;
    @Nullable
    private String fieldOfStudy;
    @Nullable
    private String school;
    @Nullable
    private String schoolType;
    @Nullable
    private String startYear;

    @Nullable
    public String getDegree() {
        return degree;
    }

    public void setDegree(@Nullable String degree) {
        this.degree = degree;
    }

    @Nullable
    public String getEndYear() {
        return endYear;
    }

    public void setEndYear(@Nullable String endYear) {
        this.endYear = endYear;
    }

    @Nullable
    public String getFieldOfStudy() {
        return fieldOfStudy;
    }

    public void setFieldOfStudy(@Nullable String fieldOfStudy) {
        this.fieldOfStudy = fieldOfStudy;
    }

    @Nullable
    public String getSchool() {
        return school;
    }

    public void setSchool(@Nullable String school) {
        this.school = school;
    }

    @Nullable
    public String getSchoolType() {
        return schoolType;
    }

    public void setSchoolType(@Nullable String schoolType) {
        this.schoolType = schoolType;
    }

    @Nullable
    public String getStartYear() {
        return startYear;
    }

    public void setStartYear(@Nullable String startYear) {
        this.startYear = startYear;
    }
}
