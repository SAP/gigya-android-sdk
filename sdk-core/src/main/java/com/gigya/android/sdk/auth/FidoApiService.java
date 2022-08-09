package com.gigya.android.sdk.auth;

public class FidoApiService {

    public enum FidoApiServiceCodes {

        REQUEST_CODE_INVALID(-1),
        REQUEST_CODE_REGISTER(1),
        REQUEST_CODE_SIGN(2),
        REQUEST_CODE_REVOKE(3);

        private final int code;

        FidoApiServiceCodes(int code) {
            this.code = code;
        }

        public int code() {
            return this.code;
        }
    }
}
