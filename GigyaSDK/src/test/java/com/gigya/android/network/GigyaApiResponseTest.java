package com.gigya.android.network;

import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.utils.FileUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;


public class GigyaApiResponseTest {

    private String mockJson() throws IOException, JSONException {
        InputStream in = Objects.requireNonNull(this.getClass().getClassLoader()).getResourceAsStream("gigyaResponseMock.json");
        String json = FileUtils.streamToString(in);
        return new JSONObject(json).toString();
    }

    private GigyaApiResponse mockResponse() throws IOException, JSONException {
        return new GigyaApiResponse(mockJson());
    }

    private GigyaApiResponse response;

    @Before
    public void setup() throws IOException, JSONException {
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
        Assert.assertNull(response.getField("nonExistent"));
    }

    @Test
    public void testGetFieldWithCorrectClassReference() {
        Assert.assertNotNull(response.getField("profile"));
        Assert.assertTrue(response.getField("profile") instanceof JSONObject);
    }

    @Test
    public void testGetFieldWithCorrectJSONObjectChild() {
        Assert.assertNotNull(response.getField("firstName"));
        Assert.assertTrue(response.getField("firstName") instanceof String);
    }

    @Test
    public void testGetFieldWithIncorrectClassReference() {
        Assert.assertNotNull(response.getField("firstName"));
        Assert.assertFalse(response.getField("firstName") instanceof Integer);
    }

}
