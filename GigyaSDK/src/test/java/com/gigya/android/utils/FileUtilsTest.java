package com.gigya.android.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;

import com.gigya.android.sdk.utils.FileUtils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.when;

public class FileUtilsTest {

    @Mock
    private
    Context context;

    @Mock
    private
    AssetManager assetManager;

    @Mock
    private ApplicationInfo applicationInfo;

    @Mock
    private Bundle bundle;

    @Mock
    private PackageManager packageManager;

    @Before
    public void setup() throws IOException, PackageManager.NameNotFoundException {
        MockitoAnnotations.initMocks(this);
        doReturn(assetManager).when(context).getAssets();
        PowerMockito.when(assetManager.open(anyString())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                return Objects.requireNonNull(this.getClass().getClassLoader()).getResourceAsStream((String) invocation.getArguments()[0]);
            }
        });
        /// Android specific mocks.
        when(context.getPackageManager()).thenReturn(packageManager);
        when(context.getPackageName()).thenReturn("");
        when(packageManager.getApplicationInfo(anyString(), anyInt())).thenReturn(applicationInfo);
        applicationInfo.metaData = bundle;
        when(bundle.get(anyString())).thenReturn("MOCK");
    }

    @Test
    public void testStreamToString() throws IOException {
        // Arrange
        final InputStream in = Objects.requireNonNull(this.getClass().getClassLoader()).getResourceAsStream("gigyaSdkConfigurationMock.json");
        if (in == null) {
            fail("Failed to load mock file");
        }
        // Act
        final String output = FileUtils.streamToString(in);
        // Assert
        assertNotNull(output);
    }

    @Test
    public void testAssetJsonFileToString() throws IOException {
        // Act
        final String output = FileUtils.assetJsonFileToString(context, "gigyaSdkConfigurationMock.json");
        // Assert
        assertNotNull(output);
    }

    @Test
    public void testLoadConfiguration() throws IOException {
        // Arrange
        PowerMockito.when(assetManager.open(anyString())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                return Objects.requireNonNull(this.getClass().getClassLoader()).getResourceAsStream("gigyaSdkConfigurationMock.json");
            }
        });
        // Act
        final String output = FileUtils.loadConfigurationJSON(context);
        // Assert
        assertNotNull(output);
    }

    @Test
    public void testStringFromMetaData() {
        // Act
        final String mockMetaDataString = FileUtils.stringFromMetaData(context, "MetaDataTag");
        // Assert
        assertNotNull(mockMetaDataString);
        assertEquals("MOCK", mockMetaDataString);
    }
}
