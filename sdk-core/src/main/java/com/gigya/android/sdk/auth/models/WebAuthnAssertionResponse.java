package com.gigya.android.sdk.auth.models;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class WebAuthnAssertionResponse {

    public String clientDataJSONBase64;
    public String authenticatorDataBase64;
    public String signatureBase64;
    public String idBase64;
    public String rawIdBase64;

    private final Gson gson = new Gson();

    public WebAuthnAssertionResponse(
            String clientDataJSONBase64,
            String authenticatorDataBase64,
            String signatureBase64,
            String idBase64,
            String rawIdBase64) {
        this.clientDataJSONBase64 = clientDataJSONBase64;
        this.authenticatorDataBase64 = authenticatorDataBase64;
        this.signatureBase64 = signatureBase64;
        this.idBase64 = idBase64;
        this.rawIdBase64 = rawIdBase64;
    }

    private Map<String, String> getResponse() {
        Map<String, String> response = new HashMap<>();
        response.put("authenticatorData", this.authenticatorDataBase64);
        response.put("clientDataJSON", this.clientDataJSONBase64);
        response.put("signature", signatureBase64);
        response.put("userHandle", rawIdBase64); //TODO user keyhandle or rawid like google requests.
        return response;
    }

    public String getAssertion() {
        Map<String, Object> assertion = new HashMap<>();
        assertion.put("id", idBase64);
        assertion.put("rawId", rawIdBase64);
        assertion.put("type", "public-key");
        assertion.put("response", getResponse());
        return gson.toJson(assertion);
    }

}
