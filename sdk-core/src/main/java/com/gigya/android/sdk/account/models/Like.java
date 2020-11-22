package com.gigya.android.sdk.account.models;

import androidx.annotation.Nullable;

public class Like {

    @Nullable
    private String category;
    @Nullable
    private String id;
    @Nullable
    private String name;
    @Nullable
    private String time;
    @Nullable
    private Long timestamp;

    @Nullable
    public String getCategory() {
        return category;
    }

    public void setCategory(@Nullable String category) {
        this.category = category;
    }

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
    public String getTime() {
        return time;
    }

    public void setTime(@Nullable String time) {
        this.time = time;
    }

    @Nullable
    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(@Nullable Long timestamp) {
        this.timestamp = timestamp;
    }
}
