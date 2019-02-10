package com.gigya.android.network;

import com.gigya.android.sdk.network.GigyaError;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class GigyaErrorTest {

    @Test
    public void validateError() {
        // Arrange/Act
        GigyaError error = new GigyaError(244,
                "Error here!",
                "98ec398b714d44f6a067163f864ee4f0");
        // Assert
        Assert.assertNotEquals(error.getErrorCode(), 0);
        Assert.assertNotNull(error.getLocalizedMessage());
        Assert.assertNotNull(error.getCallId());
        Assert.assertNotNull(error.toString());
    }

    @Test
    public void testErrorFromMap() {
        // Arrange
        Map<String, Object> errorMap = new HashMap<String, Object>() {{
            put("errorMessage", "Invalid login or password");
            put("errorCode", "403042");
        }};
        // Act.
        GigyaError error = GigyaError.errorFrom(errorMap);
        // Assert.
        Assert.assertEquals(403042, error.getErrorCode());
        Assert.assertNotNull(error.getLocalizedMessage());
        Assert.assertNull(error.getCallId());
    }
}
