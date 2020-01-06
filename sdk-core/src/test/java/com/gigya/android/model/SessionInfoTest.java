package com.gigya.android.model;

import com.gigya.android.sdk.session.SessionInfo;

import org.junit.Assert;
import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class SessionInfoTest {

    private final String MOCK_API_KEY = "3_eP-lTMvtVwgjBCKCWPgYfeWH4xVkD5Rga15I7aoVvo-S_J5ZRBLg9jLDgJvDJZag";
    private final String MOCK_SECRET = "dummy_secret";


    @Test
    public void testShortConstructor() {
        // Act
        SessionInfo session = new SessionInfo(MOCK_API_KEY, MOCK_SECRET);
        // Assert
        Assert.assertEquals(session.getExpirationTime(), 0);
    }

    @Test
    public void testLongConstructor() {
        // Arrange
        long expiration = 5;
        // Act
        SessionInfo session = new SessionInfo(MOCK_API_KEY, MOCK_SECRET, expiration);
        // Assert
        Assert.assertTrue(session.isValid());
    }

    @Test
    public void testSessionIsValid() {
        // Arrange
        long expiration = 5;
        SessionInfo session = new SessionInfo(MOCK_API_KEY, MOCK_SECRET, expiration);
        // Act
        final boolean isValid = session.isValid();
        // Assert
        assertTrue(isValid);
    }

    @Test
    public void testSessionInvalidWithNullSecret() {
        // Arrange
        long expiration = 5;
        SessionInfo session = new SessionInfo(null, MOCK_SECRET, expiration);
        // Act
        final boolean isValid = session.isValid();
        // Assert
        assertFalse(isValid);
    }

    @Test
    public void testSessionInvalidWithNullApiKey() {
        // Arrange
        long expiration = 5;
        SessionInfo session = new SessionInfo(MOCK_SECRET, null, expiration);
        // Act
        final boolean isValid = session.isValid();
        // Assert
        assertFalse(isValid);
    }

}
