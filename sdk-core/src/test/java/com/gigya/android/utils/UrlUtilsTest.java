package com.gigya.android.utils;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNull;

import com.gigya.android.sdk.utils.UrlUtils;

import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("ALL")
public class UrlUtilsTest {

    @Test
    public void testUrlEncode() {
        // Arrange
        final String basicString = "https://socialize.us1.gigya.com/socialize.getAccountInfo?UID=someId&someParameter=*ar%7Etime";
        final String encodedString = "https%3A%2F%2Fsocialize.us1.gigya.com%2Fsocialize.getAccountInfo%3FUID%3DsomeId%26someParameter%3D%2Aar%257Etime";
        // Act
        final String outpout = UrlUtils.urlEncode(basicString);
        // Assert
        assertNotNull(outpout);
        assertFalse(outpout.equals(""));
        assertEquals(outpout, encodedString);
    }

    @Test
    public void test_urlEncodeWithNullValue() {
        // Act
        final String outpout = UrlUtils.urlEncode(null);
        // Assert
        assertNull(outpout);
    }

    @Test
    public void testBuildEncodedQueryWithEmptyParams() {
        // Arrange
        final Map<String, Object> params = new HashMap<String, Object>();
        // Act
        final String outpout = UrlUtils.buildEncodedQuery(params);
        // Assert
        assertEquals(outpout, "");
    }

    @Test
    public void testBuildEncodedQuery() {
        // Arrage
        final Map<String, Object> params = new HashMap<String, Object>() {{
            put("ApiKey", "someApiKey");
            put("profile", "{ \"firstName\":\"John\", \"lastName\":\"Doe\"}");
        }};
        final String encodedQuery = "ApiKey=someApiKey&profile=%7B%20%22firstName%22%3A%22John%22%2C%20%22lastName%22%3A%22Doe%22%7D";
        // Act
        final String outpout = UrlUtils.buildEncodedQuery(params);
        // Assert
        assertNotNull(outpout);
        assertFalse(outpout.equals(""));
        assertEquals(outpout, encodedQuery);
    }

    @Test
    public void testParseUrl() {
        // Arrage
        final String toParse = "http://result/?provider=facebook&displayName=Facebook";
        // Act
        final Map<String, Object> map = UrlUtils.parseUrlParameters(toParse);
        // Assert
        assertTrue(map.size() == 2);
        assertEquals(map.get("provider"), "facebook");
        assertEquals(map.get("displayName"), "Facebook");
    }

    @Test
    public void testParseUrlParameters() {
        // Arrange
        final String toParse = "callbackID=1548684488840_0.4937520447293755&param1=1&param2=two";
        // Act
        final Map<String, Object> map = new HashMap<>();
        UrlUtils.parseUrlParameters(map, toParse);
        // Assert
        assertTrue(map.size() > 1);
        assertNotNull(map.get("callbackID"));
        assertNotNull(map.get("param1"));
        assertEquals("1", map.get("param1"));
        assertNotNull(map.get("param2"));
        assertEquals("two", map.get("param2"));
    }

    @Test
    public void testPraseUrlParametersWithNullUrl() {
        // Arrange
        final String toParse = null;
        // Act
        final Map<String, Object> map = new HashMap<>();
        UrlUtils.parseUrlParameters(map, toParse);
        // Assert
        assertTrue(map.size() == 0);
    }

    @Test
    public void testGetBaseUrl() {
        // Arrage
        final String api = "socialize.getSdkConfig";
        final String domain = "us1.gigya.com";
        // Act
        final String baseUrl = UrlUtils.getBaseUrl(api, domain);
        // Assert
        assertEquals("https://socialize.us1.gigya.com/socialize.getSdkConfig", baseUrl);
    }

    @Test
    public void testGzipDecode() throws IOException {
        // Arrage
        final byte[] encoded = new byte[]{31, -117, 8, 0, 0, 0, 0, 0, 0, 0, -85, -54, 44, 8, 73, 45, 46, 1, 0, 104, -32, 84, 90, 7, 0, 0, 0};
        // Act
        final String decoded = UrlUtils.gzipDecode(encoded).trim();
        // Assert
        assertEquals("zipTest", decoded);
    }
}
