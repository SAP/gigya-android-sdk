package com.gigya.android.sdk.auth;

import java.util.Map;

public interface IWebAuthnService {

    void initRegistration();

    void registerCredentials(Map<String, Object> params);

    void getAssertionOptions();

    void verifyAssertion(Map<String, Object> params);
}
