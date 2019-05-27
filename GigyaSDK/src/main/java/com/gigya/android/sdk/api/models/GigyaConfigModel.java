package com.gigya.android.sdk.api.models;


import com.gigya.android.sdk.network.GigyaResponseModel;

/**
 * Gigya SDK main configuration model.
 */
public class GigyaConfigModel extends GigyaResponseModel {

    private Ids ids;

    public Ids getIds() {
        return this.ids;
    }

    public static class Ids {

        private String gmid;
        private String ucid;

        public String getGmid() {
            return this.gmid;
        }

        public String getUcid() {
            return this.ucid;
        }
    }
}
