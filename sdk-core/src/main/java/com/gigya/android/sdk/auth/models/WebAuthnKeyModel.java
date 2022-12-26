package com.gigya.android.sdk.auth.models;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class WebAuthnKeyModel {

    public WebAuthnKeyModel(String name,
                            String displayName,
                            String uid,
                            String type,
                            String key) {
        this.name = name;
        this.uid = uid;
        this.displayName = displayName;
        this.type = type;
        this.key = key;
    }

    public String name;
    public String uid;
    public String displayName;
    public String type;
    public String key;


    public static List<WebAuthnKeyModel> parseList(String json) {
        if (json == null) {
            return new ArrayList<>();
        }
        Type listType = new TypeToken<ArrayList<WebAuthnKeyModel>>() {
        }.getType();
        return new Gson().<ArrayList<WebAuthnKeyModel>>fromJson(json, listType);
    }

    public static String toJsonList(List<WebAuthnKeyModel> list) {
        Type listType = new TypeToken<ArrayList<WebAuthnKeyModel>>() {
        }.getType();
        return new Gson().toJson(list, listType);
    }

}
