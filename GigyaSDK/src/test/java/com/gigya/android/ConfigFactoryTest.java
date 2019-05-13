package com.gigya.android;

import com.gigya.android.sdk.ConfigFactory;
import com.gigya.android.sdk.utils.FileUtils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ConfigFactoryTest {
    // TODO: #baryo - need to complete

    public ConfigFactory configFactory;

    @Mock
    private FileUtils mFileUtils;

    @Before
    public void setup() {
        configFactory = new ConfigFactory(mFileUtils);
    }

    @Test
    public void testLoadFromJsonFile() {}

    @Test
    public void testJsonFileDoNotExist_fallbackToManifest() {}

    @Test
    public void testErrorLoadingJsonFile_fallbackToManifest() {}

    @Test
    public void testJsonFileInvalidFormat_fallbackToManifest() {}

    @Test
    public void testLoadFromManifest() {}

}
