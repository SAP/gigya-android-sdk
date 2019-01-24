package com.gigya.android.login.provider;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.gigya.android.sdk.login.provider.LineLoginProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.when;

public class LineLoginProviderTest {

    private static final String MOCK_CHANNEL_ID = "mockChannelId";

    @Mock
    private
    Context context;

    @Mock
    private ApplicationInfo applicationInfo;

    @Mock
    private Bundle bundle;

    @Mock
    private PackageManager packageManager;

    @Before
    public void setup() throws PackageManager.NameNotFoundException {
        MockitoAnnotations.initMocks(this);
        /// Android specific mocks.
        when(context.getPackageManager()).thenReturn(packageManager);
        when(context.getPackageName()).thenReturn("");
        when(packageManager.getApplicationInfo(anyString(), anyInt())).thenReturn(applicationInfo);
        applicationInfo.metaData = bundle;
        when(bundle.get(anyString())).thenReturn("MOCK_CHANNEL_ID");
    }

    @Test
    public void testGetProviderSessionsForRequest() {
        // Arrange
        final String mockToken = "mockToken123123";
        // Act
        final LineLoginProvider loginProvider = new LineLoginProvider(null);
        final String providerSessions = loginProvider.getProviderSessionsForRequest(mockToken, -1, null);
        // Assert
        assertEquals("{\"line\":{\"authToken\":\"mockToken123123\"}}", providerSessions);
    }
}
