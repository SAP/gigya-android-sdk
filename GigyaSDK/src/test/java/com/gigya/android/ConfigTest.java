package com.gigya.android;

import com.gigya.android.sdk.Config;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertNull;
import static junit.framework.TestCase.assertEquals;

public class ConfigTest {
    private Config config;

    @Before
    public void setup() {
        config = new Config();
    }

    @Test
    public void testInit() {
        assertNull(config.getApiDomain());
        assertNull(config.getApiKey());
    }

    @Test
    public void testUpdateApiKeyAndDomain() {
        // Act
        config.updateWith(StaticMockFactory.API_KEY, StaticMockFactory.API_DOMAIN);

        // Assert
        assertEquals(StaticMockFactory.API_DOMAIN, config.getApiDomain());
        assertEquals(StaticMockFactory.API_KEY, config.getApiKey());
    }

    @Test
    public void testUpdateAll() {
        // Act
        config.updateWith(StaticMockFactory.API_KEY, StaticMockFactory.API_DOMAIN, 2, 5);

        // Assert
        assertEquals(StaticMockFactory.API_DOMAIN, config.getApiDomain());
        assertEquals(StaticMockFactory.API_KEY, config.getApiKey());
        assertEquals(2, config.getAccountCacheTime());
        assertEquals(5, config.getSessionVerificationInterval());
    }

    @Test
    public void testUpdateFromAnotherInstance() {
        // Arrange
        Config newConfig = new Config();
        newConfig.setAccountCacheTime(7);
        newConfig.setApiKey("123123");
        newConfig.setApiDomain("testDomain");

        // Act
        config.updateWith(newConfig);

        // Assert
        assertEquals(7, config.getAccountCacheTime());
        assertEquals("123123", config.getApiKey());
        assertEquals("testDomain", config.getApiDomain());
    }
}
