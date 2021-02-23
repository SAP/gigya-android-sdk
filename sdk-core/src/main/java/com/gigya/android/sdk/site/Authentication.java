package com.gigya.android.sdk.site;

import java.util.Map;

enum AuthMethodType {
    password, push
}

public class Authentication {
    public Map<AuthMethodType, AuthMethod> methods;
}
