package com.gigya.android.network;

import com.gigya.android.sdk.network.GigyaError;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GigyaErrorTest {

    private GigyaError error;

    @Before
    public void buildMock() {
        error = new GigyaError(244,
                "Error here!",
                "98ec398b714d44f6a067163f864ee4f0");
    }

    @Test
    public void validateError() {
        Assert.assertNotEquals(error.getErrorCode(), 0);
        Assert.assertNotNull(error.getLocalizedMessage());
        Assert.assertNotNull(error.getCallId());
        Assert.assertNotNull(error.toString());
    }
}
