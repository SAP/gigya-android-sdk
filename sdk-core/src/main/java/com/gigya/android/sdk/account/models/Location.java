package com.gigya.android.sdk.account.models;

import android.support.annotation.Nullable;

public class Location {

    @Nullable
    private String city;
    @Nullable
    private String country;
    @Nullable
    private String state;
    @Nullable
    private Coordinates coordinates;

    @Nullable
    public String getCity() {
        return city;
    }

    public void setCity(@Nullable String city) {
        this.city = city;
    }

    @Nullable
    public String getCountry() {
        return country;
    }

    public void setCountry(@Nullable String country) {
        this.country = country;
    }

    @Nullable
    public String getState() {
        return state;
    }

    public void setState(@Nullable String state) {
        this.state = state;
    }

    @Nullable
    public Coordinates getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(@Nullable Coordinates coordinates) {
        this.coordinates = coordinates;
    }
}
