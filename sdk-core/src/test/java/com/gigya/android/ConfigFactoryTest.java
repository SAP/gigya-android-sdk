package com.gigya.android;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import android.os.Bundle;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.ConfigFactory;
import com.gigya.android.sdk.utils.FileUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Bundle.class})
public class ConfigFactoryTest {

    public ConfigFactory configFactory;

    @Mock
    private FileUtils mFileUtils;

    @Before
    public void setup() {
        configFactory = new ConfigFactory(mFileUtils);
        mockLoadFromManifest();
    }

    private void mockLoadFromManifest() {
        final Bundle bundle = mock(Bundle.class);
        when(mFileUtils.getMetaData()).thenReturn(bundle);
        when(bundle.getString("apiKey", null)).thenReturn("3_l7zxNcj4vhu8tLzYafUnKDSA4VsOVNzR4VnclcC6VKsXXmQdq950uC-zY7Vsu9RC");
        when(bundle.getString("apiDomain", "us1.gigya.com")).thenReturn("us1.gigya.com");
        when(bundle.getInt("accountCacheTime", 5)).thenReturn(5);
        when(bundle.getInt("sessionVerificationInterval", 0)).thenReturn(0);
    }


    @Test
    public void testLoadFromJsonFile() throws IOException {
        // Arrange
        when(mFileUtils.containsFile(anyString())).thenReturn(true);
        when(mFileUtils.loadFile(anyString())).thenReturn(StaticMockFactory.getMockConfigurationFileJson());
        // Act
        final Config config = configFactory.loadFromJson();
        // Assert
        assertEquals("3_l7zxNcj4vhu8tLzYafUnKDSA4VsOVNzR4VnclcC6VKsXXmQdq950uC-zY7Vsu9RC", config.getApiKey());
        assertEquals("us1.gigya.com", config.getApiDomain());
        assertEquals(1, config.getAccountCacheTime());
        assertEquals(0, config.getSessionVerificationInterval());
    }


    @Test
    public void testJsonFileDoNotExist_fallbackToManifest() {
        // Arrange
        when(mFileUtils.containsFile(anyString())).thenReturn(false);
        // Act
        final Config config = configFactory.load();
        // Assert
        assertEquals("3_l7zxNcj4vhu8tLzYafUnKDSA4VsOVNzR4VnclcC6VKsXXmQdq950uC-zY7Vsu9RC", config.getApiKey());
        assertEquals("us1.gigya.com", config.getApiDomain());
        assertEquals(5, config.getAccountCacheTime());
        assertEquals(0, config.getSessionVerificationInterval());
    }

    @Test
    public void testErrorLoadingJsonFile_fallbackToManifest() throws IOException {
        // Arrange
        when(mFileUtils.containsFile(anyString())).thenReturn(true);
        when(mFileUtils.loadFile(anyString())).thenReturn(StaticMockFactory.getMockConfigurationFileJson().substring(0, 20));
        // Act
        final Config config = configFactory.load();
        // Assert
        assertEquals("3_l7zxNcj4vhu8tLzYafUnKDSA4VsOVNzR4VnclcC6VKsXXmQdq950uC-zY7Vsu9RC", config.getApiKey());
        assertEquals("us1.gigya.com", config.getApiDomain());
        assertEquals(5, config.getAccountCacheTime());
        assertEquals(0, config.getSessionVerificationInterval());
    }


    @Test
    public void testLoadFromManifest() {
        // Arrange
        when(mFileUtils.containsFile(anyString())).thenReturn(true);
        // Act.
        final Config config = configFactory.loadFromManifest();
        // Assert
        assertEquals("3_l7zxNcj4vhu8tLzYafUnKDSA4VsOVNzR4VnclcC6VKsXXmQdq950uC-zY7Vsu9RC", config.getApiKey());
        assertEquals("us1.gigya.com", config.getApiDomain());
        assertEquals(5, config.getAccountCacheTime());
        assertEquals(0, config.getSessionVerificationInterval());
    }

}
