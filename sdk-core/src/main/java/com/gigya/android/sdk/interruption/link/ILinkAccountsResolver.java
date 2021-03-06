package com.gigya.android.sdk.interruption.link;

import com.gigya.android.sdk.interruption.link.models.ConflictingAccounts;

public interface ILinkAccountsResolver {

    ConflictingAccounts getConflictingAccounts();

    void requestConflictingAccounts();

    void linkToSite(String loginID, String password);

    void linkToSocial(String providerName);
}
