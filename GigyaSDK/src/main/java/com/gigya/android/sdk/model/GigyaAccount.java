package com.gigya.android.sdk.model;

public class GigyaAccount extends BaseGigyaAccount {

    // TODO: 23/12/2018 Data is not one of the concrete account fields. It is added when creating the screet sets. Remove it!

    private Data data;

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }
}
