package com.gigya.android.sdk.auth.models;

import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;

public class WebAuthnAssertionResponse {

    public Object userHandleBase64;
    public String clientDataJSONBase64;
    public String authenticatorDataBase64;
    public String signatureBase64;
    public String idBase64;
    public String rawIdBase64;

    public WebAuthnAssertionResponse(
            Object userHandleBase64,
            String clientDataJSONBase64,
            String authenticatorDataBase64,
            String signatureBase64,
            String idBase64,
            String rawIdBase64) {
        this.userHandleBase64 = userHandleBase64;
        this.clientDataJSONBase64 = clientDataJSONBase64;
        this.authenticatorDataBase64 = authenticatorDataBase64;
        this.signatureBase64 = signatureBase64;
        this.idBase64 = idBase64;
        this.rawIdBase64 = rawIdBase64;
    }

    private Map<String, Object> getResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("authenticatorData", this.authenticatorDataBase64);
        response.put("clientDataJSON", this.clientDataJSONBase64);
        response.put("signature", signatureBase64);
        response.put("userHandle", userHandleBase64); // Usually null (optional).
        return response;
    }

    public String getAssertion() {
        Map<String, Object> assertion = new HashMap<>();
        assertion.put("id", idBase64);
        assertion.put("rawId", rawIdBase64);
        assertion.put("type", "public-key");
        assertion.put("response", getResponse());

        final GsonBuilder builder = new GsonBuilder().serializeNulls();
        return builder.create().toJson(assertion);
    }

}
