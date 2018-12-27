package com.gigya.android;

import com.gigya.android.sdk.utils.UrlUtils;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;

@SuppressWarnings("ALL")
public class UrlUtilsTest {

    @Test
    public void testUrlEncode() {
        final String basicString = "https://socialize.us1.gigya.com/socialize.getAccountInfo?UID=someId&someParameter=*ar%7Etime";
        final String outpout = UrlUtils.urlEncode(basicString);
        assertNotNull(outpout);
        assertFalse(outpout.equals(""));
        System.out.println(outpout);
        final String encodedString = "https%3A%2F%2Fsocialize.us1.gigya.com%2Fsocialize.getAccountInfo%3FUID%3DsomeId%26someParameter%3D%2Aar%257Etime";
        assertEquals(outpout, encodedString);
    }

    @Test
    public void testBuildEncodedQueryWithEmptyParams() {
        Map<String, Object> params = new HashMap<String, Object>();
        final String outpout = UrlUtils.buildEncodedQuery(params);
        assertEquals(outpout, "");
    }

    @Test
    public void testBuildEncodedQuery() {
        Map<String, Object> params = new HashMap<String, Object>() {{
            put("ApiKey", "someApiKey");
            put("profile", "{ \"firstName\":\"John\", \"lastName\":\"Doe\"}");
        }};
        final String outpout = UrlUtils.buildEncodedQuery(params);
        assertNotNull(outpout);
        assertFalse(outpout.equals(""));
        System.out.println(outpout);
        final String encodedQuery = "ApiKey=someApiKey&profile=%7B%20%22firstName%22%3A%22John%22%2C%20%22lastName%22%3A%22Doe%22%7D";
        assertEquals(outpout, encodedQuery);
    }
}
