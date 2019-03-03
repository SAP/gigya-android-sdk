package com.gigya.android.sdk.model.profile;

import android.support.annotation.Nullable;

public class Favorite {

    @Nullable
    private String id;
    @Nullable
    private String name;
    @Nullable
    private String category;

    @Nullable
    public String getId() {
        return id;
    }

    public void setId(@Nullable String id) {
        this.id = id;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    @Nullable
    public String getCategory() {
        return category;
    }

    public void setCategory(@Nullable String category) {
        this.category = category;
    }
}
