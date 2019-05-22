package com.gigya.android.sdk.interruption.link;

import com.gigya.android.sdk.model.account.ConflictingAccounts;

public interface IGigyaLinkAccountsResolver {

    ConflictingAccounts getConflictingAccounts();

    void linkToSite(String loginID, String password);

    void linkToSocial(String providerName);

    void clear();
}
