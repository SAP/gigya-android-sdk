package com.gigya.android.sdk.account.models;

import android.support.annotation.Nullable;

public class Coordinates {

    @Nullable
    private Float lat;
    @Nullable
    private Float lon;

    @Nullable
    public Float getLat() {
        return lat;
    }

    public void setLat(@Nullable Float lat) {
        this.lat = lat;
    }

    @Nullable
    public Float getLon() {
        return lon;
    }

    public void setLon(@Nullable Float lon) {
        this.lon = lon;
    }
}
