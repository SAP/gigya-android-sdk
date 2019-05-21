package com.gigya.android.sdk.model.account;

/*
Holder class required for DI of the account schema (Class<> is final)
 */
public class GigyaAccountClass<T extends GigyaAccount> {
    public static final GigyaAccountClass<GigyaAccount> Default = new GigyaAccountClass<>(GigyaAccount.class);

    private Class<T> _accountClass;
    public Class<T> getAccountClass() { return this._accountClass; }

    public GigyaAccountClass(Class<T> clazz) { _accountClass = clazz; }
}
