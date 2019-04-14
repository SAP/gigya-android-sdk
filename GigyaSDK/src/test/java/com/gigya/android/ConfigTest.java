package com.gigya.android;

import com.gigya.android.sdk.Config;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class ConfigTest {

    @Test
    public void testUpdate() {
        // Act
        Config cConfig = new Config();
        cConfig.updateWith(StaticMockFactory.API_KEY, StaticMockFactory.API_DOMAIN);
        // Assert
        assertEquals(StaticMockFactory.API_DOMAIN, cConfig.getApiDomain());
        assertEquals(StaticMockFactory.API_KEY, cConfig.getApiKey());

        // Act
        cConfig.updateWith(StaticMockFactory.API_KEY, StaticMockFactory.API_DOMAIN, 2, 5);
        // Assert
        assertEquals(StaticMockFactory.API_DOMAIN, cConfig.getApiDomain());
        assertEquals(StaticMockFactory.API_KEY, cConfig.getApiKey());
        assertEquals(2, cConfig.getAccountCacheTime());
        assertEquals(5, cConfig.getSessionVerificationInterval());

        // Arrange
        Config newConfig = new Config();
        newConfig.setAccountCacheTime(7);
        newConfig.setApiKey("123123");
        newConfig.setApiDomain("testDomain");
        // Act
        cConfig.updateWith(newConfig);
        // Assert
        assertEquals(7, cConfig.getAccountCacheTime());
        assertEquals("123123", cConfig.getApiKey());
        assertEquals("testDomain", cConfig.getApiDomain());
    }
}
