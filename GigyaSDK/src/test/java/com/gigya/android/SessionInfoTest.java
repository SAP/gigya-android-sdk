package com.gigya.android;

import com.gigya.android.sdk.model.SessionInfo;

import org.junit.Assert;
import org.junit.Test;

public class SessionInfoTest {

    private final String MOCK_API_KEY = "3_eP-lTMvtVwgjBCKCWPgYfeWH4xVkD5Rga15I7aoVvo-S_J5ZRBLg9jLDgJvDJZag";
    private final String MOCK_SECRET = "dummy_secret";


    @Test
    public void testShortConstructor() {
        SessionInfo session = new SessionInfo(MOCK_API_KEY, MOCK_SECRET);
        Assert.assertEquals(session.getExpirationTime(), Long.MAX_VALUE);
    }

    @Test
    public void testLingConstructor() {
        long expiration = (System.currentTimeMillis() / 1000) + 10;
        SessionInfo session = new SessionInfo(MOCK_API_KEY, MOCK_SECRET, expiration);
        Assert.assertTrue(session.isValid());
    }

}
