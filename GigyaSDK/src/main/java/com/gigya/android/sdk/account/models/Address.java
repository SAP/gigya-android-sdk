package com.gigya.android.sdk.account.models;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class Address {

    @Nullable
    private String country;
    @Nullable
    private String formatted;
    @Nullable
    private String locality;
    @Nullable
    @SerializedName("postalCode")
    private String postalCode;
    @Nullable
    private String region;
    @Nullable
    @SerializedName("street_address")
    private String streetAddress;

    @Nullable
    public String getCountry() {
        return country;
    }

    public void setCountry(@Nullable String country) {
        this.country = country;
    }

    @Nullable
    public String getFormatted() {
        return formatted;
    }

    public void setFormatted(@Nullable String formatted) {
        this.formatted = formatted;
    }

    @Nullable
    public String getLocality() {
        return locality;
    }

    public void setLocality(@Nullable String locality) {
        this.locality = locality;
    }

    @Nullable
    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(@Nullable String postalCode) {
        this.postalCode = postalCode;
    }

    @Nullable
    public String getRegion() {
        return region;
    }

    public void setRegion(@Nullable String region) {
        this.region = region;
    }

    @Nullable
    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(@Nullable String streetAddress) {
        this.streetAddress = streetAddress;
    }
}
