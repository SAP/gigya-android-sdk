package com.gigya.android;

import com.gigya.android.sdk.model.SessionInfo;

import org.junit.Assert;
import org.junit.Test;

public class SessionInfoTest {

    private final String MOCK_API_KEY = "3_eP-lTMvtVwgjBCKCWPgYfeWH4xVkD5Rga15I7aoVvo-S_J5ZRBLg9jLDgJvDJZag";
    private final String MOCK_SECRET = "dummy_secret";


    @Test
    public void testShortConstructor() {
        // Act
        SessionInfo session = new SessionInfo(MOCK_API_KEY, MOCK_SECRET);
        // Assert
        Assert.assertEquals(session.getExpirationTime(), Long.MAX_VALUE);
    }

    @Test
    public void testLongConstructor() {
        // Arrange
        long expiration = System.currentTimeMillis() + 100;
        // Act
        SessionInfo session = new SessionInfo(MOCK_API_KEY, MOCK_SECRET, expiration);
        // Assert
        Assert.assertTrue(session.isValid());
    }

}
