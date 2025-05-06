package com.gigya.android.sdk.api;

import java.util.Arrays;
import java.util.List;

public class InvalidGMIDResponseEvaluator {

    private static final String DETAILS_CAUSE_MISSING_COOKIE = "missing cookie";
    private static final String DETAILS_SESSION_IS_INVALID = "Session is invalid (Missing DeviceId)";
    private static final String DETAILS_MISSING_GCID_OR_UCID = "Missing required parameter: gcid or ucid cookie";

    private static final String FLAGS_MISSING_KEY = "missingKey";

    private static final int ERROR_INVALID_PARAMETER_VALUE = 400006;

    public boolean evaluate(GigyaApiResponse response) {
        if (response.getErrorCode() == 0) {
            return false;
        }

        int errorCode = response.getErrorCode();
        String errorDetails = response.getErrorDetails();
        String errorFlags = response.getErrorFlags();

        // Check detail clauses
        List<String> detailClauses = Arrays.asList(
                DETAILS_CAUSE_MISSING_COOKIE,
                DETAILS_SESSION_IS_INVALID,
                DETAILS_MISSING_GCID_OR_UCID
        );

        if (detailClauses.contains(errorDetails)) {
            return true;
        }

        // Check error code/flag pair
        return errorCode == ERROR_INVALID_PARAMETER_VALUE && FLAGS_MISSING_KEY.equals(errorFlags);
    }
}
