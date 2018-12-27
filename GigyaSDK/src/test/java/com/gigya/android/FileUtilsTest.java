package com.gigya.android;

import android.content.Context;
import android.content.res.AssetManager;

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

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

public class FileUtilsTest {

    @Mock
    private
    Context context;

    @Mock
    private
    AssetManager assetManager;

    @Before
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);
        doReturn(assetManager).when(context).getAssets();
        PowerMockito.when(assetManager.open(anyString())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                return Objects.requireNonNull(this.getClass().getClassLoader()).getResourceAsStream((String) invocation.getArguments()[0]);
            }
        });
    }

    @Test
    public void testStreamToString() throws IOException {
        final InputStream in = Objects.requireNonNull(this.getClass().getClassLoader()).getResourceAsStream("gigyaSdkConfigurationMock.json");
        if (in == null) {
            fail("Failed to load mock file");
        }
        final String output = FileUtils.streamToString(in);
        assertNotNull(output);
        System.out.println(output);
    }

    @Test
    public void testAssetJsonFileToString() throws IOException {
        final String output = FileUtils.assetJsonFileToString(context, "gigyaSdkConfigurationMock.json");
        assertNotNull(output);
        System.out.println(output);
    }

}
