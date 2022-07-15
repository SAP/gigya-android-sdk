package com.gigya.android.sdk.auth.models;

import java.util.HashMap;
import java.util.Map;

public class WebAuthnAttestationResponse {

    public String keyHandleBase64;
    public String clientDataJSONBase64;
    public String attestationObjectBase64;
    public String idBase64;
    public String rawIdBase64;

    public WebAuthnAttestationResponse(
            String keyHandleBase64,
            String clientDataJSONBase64,
            String attestationObjectBase64,
            String idBase64,
            String rawIdBase64) {
        this.keyHandleBase64 = keyHandleBase64;
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

    public Map<String, Object> getAttestation() {
        Map<String, Object> attestation = new HashMap<>();
        attestation.put("id", this.idBase64);
        attestation.put("rawId", this.rawIdBase64);
        attestation.put("type", "public-key");
        attestation.put("response", getResponse());
        return attestation;
    }
}
