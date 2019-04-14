package com.gigya.android;

import com.gigya.android.sdk.ui.Presenter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

@RunWith(PowerMockRunner.class)
public class PresenterTest extends BaseGigyaTest {

    private Presenter cPreserner;

    @Before
    public void setup() throws Exception {
        super.setup();

        mockConfig();
        cPreserner = new Presenter(mContext, mConfig, mWebViewFragmentFactory);
    }

    @Test
    public void testGetPresentationUrl() {
        // Arrange
        final Map<String, Object> params = new HashMap<>();
        params.put("provider", "yahoo");
        // Act
        String url = cPreserner.getPresentationUrl(params, "login");
        // Assert
        assertNotNull(url);
        assertEquals("https://socialize.us1-gigya.com/gs/mobile/loginui.aspx?apiKey=3_eP-lTMvtVwgjBCKCWPgYfeWH4xVkD5Rga15I7aoVvo-S_J5ZRBLg9jLDgJvDJZag&requestType=login&sdk=android_4.0.0&redirect_uri=gsapi%3A%2F%2Fresult%2F", url);
    }

    @Test
    public void testGetPresentationUrlComplex() {
        // Arrange
        final Map<String, Object> params = new HashMap<>();
        params.put("provider", "yahoo");
        params.put("enabledProviders", "facebook");
        params.put("disabledProviders", "googleplus");
        params.put("lang", "eng");
        params.put("cid", "123123");
        // Act
        String url = cPreserner.getPresentationUrl(params, "login");
        // Assert
        assertNotNull(url);
        assertEquals("https://socialize.us1-gigya.com/gs/mobile/loginui.aspx?enabledProviders=facebook&apiKey=3_eP-lTMvtVwgjBCKCWPgYfeWH4xVkD5Rga15I7aoVvo-S_J5ZRBLg9jLDgJvDJZag&requestType=login&disabledProviders=googleplus&sdk=android_4.0.0&redirect_uri=gsapi%3A%2F%2Fresult%2F&lang=eng&cid=123123", url);
    }
}
