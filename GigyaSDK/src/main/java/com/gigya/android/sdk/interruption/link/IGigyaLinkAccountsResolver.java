package com.gigya.android.sdk.interruption.link;

import android.content.Context;

import com.gigya.android.sdk.model.account.ConflictingAccounts;

public interface IGigyaLinkAccountsResolver {

    ConflictingAccounts getConflictingAccounts();

    void linkToSite(String loginID, String password);

    void linkToSocial(final Context context, String providerName);

    void clear();
}
