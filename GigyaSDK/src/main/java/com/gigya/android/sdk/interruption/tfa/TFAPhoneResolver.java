package com.gigya.android.sdk.interruption.tfa;

import com.gigya.android.sdk.AccountManager;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.interruption.GigyaResolver;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

public class TFAPhoneResolver extends GigyaResolver {

    private final String regToken;

    public TFAPhoneResolver(Configuration configuration, NetworkAdapter networkAdapter, SessionManager sessionManager, AccountManager accountManager, String regToken) {
        super(configuration, networkAdapter, sessionManager, accountManager);
        this.regToken = regToken;
    }
}
