package com.gigya.android.sdk.account.models;

import android.support.annotation.Nullable;

import java.util.List;

public class Favorites {

    @Nullable
    private List<Favorite> activities;
    @Nullable
    private List<Favorite> books;
    @Nullable
    private List<Favorite> interests;
    @Nullable
    private List<Favorite> movies;
    @Nullable
    private List<Favorite> music;
    @Nullable
    private List<Favorite> television;

    @Nullable
    public List<Favorite> getActivities() {
        return activities;
    }

    public void setActivities(@Nullable List<Favorite> activities) {
        this.activities = activities;
    }

    @Nullable
    public List<Favorite> getBooks() {
        return books;
    }

    public void setBooks(@Nullable List<Favorite> books) {
        this.books = books;
    }

    @Nullable
    public List<Favorite> getInterests() {
        return interests;
    }

    public void setInterests(@Nullable List<Favorite> interests) {
        this.interests = interests;
    }

    @Nullable
    public List<Favorite> getMovies() {
        return movies;
    }

    public void setMovies(@Nullable List<Favorite> movies) {
        this.movies = movies;
    }

    @Nullable
    public List<Favorite> getMusic() {
        return music;
    }

    public void setMusic(@Nullable List<Favorite> music) {
        this.music = music;
    }

    @Nullable
    public List<Favorite> getTelevision() {
        return television;
    }

    public void setTelevision(@Nullable List<Favorite> television) {
        this.television = television;
    }
}
