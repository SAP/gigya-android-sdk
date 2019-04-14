package com.gigya.android.network;

import com.gigya.android.StaticMockFactory;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.network.GigyaApiResponse;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertNotNull;
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
        Assert.assertTrue(response.containsNested("profile"));
        Assert.assertTrue(response.containsNested("profile.firstName"));
        Assert.assertTrue(response.containsNested("profile.lastName"));
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
