package com.gigya.android.network;

import com.gigya.android.StaticMockFactory;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.network.GigyaApiResponse;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;


public class GigyaApiResponseTest {

    private GigyaApiResponse mockResponse() throws JSONException {
        return new GigyaApiResponse(StaticMockFactory.getMockAccountJson());
    }

    private GigyaApiResponse response;

    @Before
    public void setup() throws JSONException {
        response = mockResponse();
    }

    @Test
    public void testCallId() {
        Assert.assertNotEquals(response.getCallId(), "");
    }

    @Test
    public void testStatusCode() {
        Assert.assertNotEquals(response.getStatusCode(), GigyaApiResponse.INVALID_VALUE);
    }

    @Test
    public void testErrorCode() {
        Assert.assertNotEquals(response.getErrorCode(), GigyaApiResponse.INVALID_VALUE);
    }

    @Test
    public void testErrorDetails() {

        Assert.assertNotEquals(response.getErrorDetails(), "");
    }

    @Test
    public void testStatusReason() {
        Assert.assertNotEquals(response.getStatusReason(), "");
    }

    @Test
    public void testTime() {

        Assert.assertNotEquals(response.getTime(), "");
    }

    @Test
    public void testGetFieldWithIncorrectFieldKey() {
        // Assert
        assertNull(response.getField("nonExistent", Object.class));
    }

    @Test
    public void testContainsNestedKey() {
        // Assert
        assertTrue(response.containsNested("profile"));
        assertTrue(response.containsNested("profile.firstName"));
        assertTrue(response.containsNested("profile.lastName"));
        assertTrue(response.containsNested("profile.gender"));

        assertFalse(response.containsNested("profile.dummy"));
    }

    @Test
    public void testContainsNestedKeyDeep() {
        // Arrange
        GigyaApiResponse dummyResp = new GigyaApiResponse("{\n" +
                "  \"dummy\": {\n" +
                "    \"flatStep\": \"dummy\",\n" +
                "    \"deep\": {\n" +
                "      \"first\": \"dummy\",\n" +
                "      \"deep\": {\n" +
                "        \"second\": \"dummy\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        // Assert
        assertTrue(dummyResp.containsNested("dummy"));
        HashMap dummy = dummyResp.getField("dummy", HashMap.class);
        assertNotNull(dummy);

        assertTrue(dummyResp.containsNested("dummy.flatStep"));
        String flatStep = dummyResp.getField("dummy.flatStep", String.class);
        assertNotNull(flatStep);

        assertTrue(dummyResp.containsNested("dummy.deep.first"));
        String first = dummyResp.getField("dummy.deep.first", String.class);
        assertNotNull(first);

        assertTrue(dummyResp.containsNested("dummy.deep.deep"));
        HashMap deep = dummyResp.getField("dummy.deep.deep",   HashMap.class);
        assertNotNull(deep);

        assertTrue(dummyResp.containsNested("dummy.deep.deep.second"));
        String second = dummyResp.getField("dummy.deep.deep.second", String.class);
        assertNotNull(second);

        assertFalse(dummyResp.containsNested("dummy.none"));
        assertFalse(dummyResp.containsNested("dummy.none.deep"));
        assertFalse(dummyResp.containsNested("dummy.none.deep.none"));
        assertFalse(dummyResp.containsNested("dummy.deep.deep.second.third"));
    }

    @Test
    public void textGetNestedField() {
        // Act
        final String field = response.getField("profile.firstName", String.class);
        // Assert
        Assert.assertNotNull(field);
    }

    @Test
    public void testGetFieldWithCorrectClassReference() {
        // Assert
        Assert.assertNotNull(response.getField("profile", Object.class));
    }

    @Test
    public void testGetFieldWithCorrectJSONObjectChild() {
        // Assert
        Assert.assertNotNull(response.getField("profile.firstName", String.class));
    }

    @Test
    public void testParseTo() {
        // Act
        GigyaAccount ga = response.parseTo(GigyaAccount.class);
        // Assert
        assertNotNull(ga);
    }

    @Test
    public void testParseToWithNullClazz() {
        // Act
        GigyaAccount ga = response.parseTo(null);
        // Assert
        assertNull(ga);
    }
}
