package com.gigya.android.sdk.auth.models;

import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class WebAuthnAttestationResponse {

    public String clientDataJSONBase64;
    public String attestationObjectBase64;
    public String idBase64;
    public String rawIdBase64;

    private final Gson gson = new Gson();

    public WebAuthnAttestationResponse(
            String clientDataJSONBase64,
            String attestationObjectBase64,
            String idBase64,
            String rawIdBase64) {
        this.clientDataJSONBase64 = clientDataJSONBase64;
        this.attestationObjectBase64 = attestationObjectBase64;
        this.idBase64 = idBase64;
        this.rawIdBase64 = rawIdBase64;
    }

    private Map<String, String> getResponse() {
        Map<String, String> response = new HashMap<>();
        response.put("attestationObject", this.attestationObjectBase64);
        response.put("clientDataJSON", this.clientDataJSONBase64);
        return response;
    }

    public String getAttestation() {
        Map<String, Object> attestation = new HashMap<>();
        attestation.put("id", this.idBase64);
        attestation.put("rawId", this.rawIdBase64);
        attestation.put("type", "public-key");
        attestation.put("response", getResponse());
        return gson.toJson(attestation);
    }
}
