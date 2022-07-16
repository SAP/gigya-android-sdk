package com.gigya.android.model;

import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.utils.AccountGSONDeserializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ProfileTest {

    final Gson gson = new GsonBuilder().registerTypeAdapter(GigyaAccount.class,
            new AccountGSONDeserializer<GigyaAccount>()).create();

    @Test
    public void testProfileWithFavoritesObjectResponse() {
        final String json = "{\n" +
                "  \"callId\": \"996f740c37114ed99489c1be391c291a\",\n" +
                "  \"errorCode\": 0,\n" +
                "  \"apiVersion\": 2,\n" +
                "  \"statusCode\": 200,\n" +
                "  \"statusReason\": \"OK\",\n" +
                "  \"time\": \"2021-07-26T12:06:53.595Z\",\n" +
                "   \"profile\": {\n" +
                "    \"firstName\": \"Some\",\n" +
                "    \"lastName\": \"Profile\",\n" +
                "    \"education\": [],\n" +
                "    \"email\": \"some.email@sap.com\",\n" +
                "    \"favorites\": {\n" +
                "      \"music\": []\n" +
                "    },\n" +
                "    \"gender\": \"u\",\n" +
                "    \"languages\": \"\"\n" +
                "  }\n" +
                "}";
        final GigyaAccount account = gson.fromJson(json, GigyaAccount.class);

        assertNotNull(account.getProfile().getFavorites());
        assertTrue(account.getProfile().getFavorites() instanceof List);
    }

    @Test
    public void testProfileWithFavoritesArrayResponse() {
        final String json = "{\n" +
                "  \"profile\": {\n" +
                "    \"firstName\": \"Rem\",\n" +
                "    \"lastName\": \"Gigya\",\n" +
                "    \"education\": [],\n" +
                "    \"email\": \"remi.cullen.sap@gmail.com\",\n" +
                "    \"favorites\": [\n" +
                "      {\n" +
                "        \"music\": [\n" +
                "          {\n" +
                "            \"id\":\"music id 1\",\n" +
                "            \"name\": \"music name 1\",\n" +
                "            \"category\" : \"music category 1\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"id\":\"music id 2\",\n" +
                "            \"name\": \"music name 2\",\n" +
                "            \"category\" : \"music category 2\"\n" +
                "          }]\n" +
                "      },\n" +
                "      {\n" +
                "        \"movies\": [\n" +
                "          {\n" +
                "            \"id\":\"movies id 1\",\n" +
                "            \"name\": \"movies name 1\",\n" +
                "            \"category\" : \"movies category 1\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"id\":\"movies id 2\",\n" +
                "            \"name\": \"movies name 2\",\n" +
                "            \"category\" : \"movies category \"\n" +
                "          }]\n" +
                "      }\n" +
                "    ],\n" +
                "    \"gender\": \"u\"\n" +
                "  }\n" +
                "}";
        final GigyaAccount account = gson.fromJson(json, GigyaAccount.class);

        assertNotNull(account.getProfile().getFavorites());
        assertTrue(account.getProfile().getFavorites() instanceof List);
        assertEquals("music id 1", account.getProfile().getFavorites().get(0).getMusic().get(0).getId());
        assertEquals("movies id 2", account.getProfile().getFavorites().get(1).getMovies().get(1).getId());
    }

    @Test
    public void testProfileWithLikesObjectResponse() {
        final String json = "{\n" +
                "   \"profile\": {\n" +
                "    \"firstName\": \"Some\",\n" +
                "    \"lastName\": \"Profile\",\n" +
                "    \"education\": [],\n" +
                "    \"email\": \"some.email@sap.com\",\n" +
                "    \"likes\": {\n" +
                "      \"name\": \"Like name\",\n" +
                "      \"category\": \"Like category\",\n" +
                "      \"id\": \"Like id\"\n" +
                "    },\n" +
                "    \"gender\": \"u\",\n" +
                "    \"languages\": \"\"\n" +
                "  }\n" +
                "}";

        final GigyaAccount account = gson.fromJson(json, GigyaAccount.class);
        assertNotNull(account.getProfile().getLikes());
        assertTrue(account.getProfile().getLikes() instanceof List);
        assertEquals("Like name", account.getProfile().getLikes().get(0).getName());
        assertEquals("Like category", account.getProfile().getLikes().get(0).getCategory());
    }

    @Test
    public void testProfileWithLikesArrayResponse() {
        final String json = "{\n" +
                "  \"profile\": {\n" +
                "    \"firstName\": \"Some\",\n" +
                "    \"lastName\": \"Profile\",\n" +
                "    \"education\": [],\n" +
                "    \"email\": \"some.email@sap.com\",\n" +
                "    \"likes\": [\n" +
                "      {\n" +
                "        \"name\": \"Like name 1\",\n" +
                "        \"category\": \"Like category 1\",\n" +
                "        \"id\": \"Like id 1\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"name\": \"Like name 2\",\n" +
                "        \"category\": \"Like category 2\",\n" +
                "        \"id\": \"Like id 2\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"gender\": \"u\",\n" +
                "    \"languages\": \"\"\n" +
                "  }\n" +
                "}";

        final GigyaAccount account = gson.fromJson(json, GigyaAccount.class);
        assertNotNull(account.getProfile().getLikes());
        assertTrue(account.getProfile().getLikes() instanceof List);
        assertEquals("Like name 1", account.getProfile().getLikes().get(0).getName());
        assertEquals("Like category 1", account.getProfile().getLikes().get(0).getCategory());
        assertEquals("Like name 2", account.getProfile().getLikes().get(1).getName());
        assertEquals("Like category 2", account.getProfile().getLikes().get(1).getCategory());
    }

    @Test
    public void testProfileWithPatentsObjectResponse() {
        final String json = "{\n" +
                "   \"profile\": {\n" +
                "    \"firstName\": \"Some\",\n" +
                "    \"lastName\": \"Profile\",\n" +
                "    \"education\": [],\n" +
                "    \"email\": \"some.email@sap.com\",\n" +
                "    \"patents\": {\n" +
                "      \"title\": \"title\",\n" +
                "      \"summary\": \"summary\",\n" +
                "      \"number\": \"number\",\n" +
                "      \"office\": \"office\",\n" +
                "      \"status\": \"status\"\n" +
                "    },\n" +
                "    \"gender\": \"u\",\n" +
                "    \"languages\": \"\"\n" +
                "  }\n" +
                "}";

        final GigyaAccount account = gson.fromJson(json, GigyaAccount.class);
        assertNotNull(account.getProfile().getPatents());
        assertTrue(account.getProfile().getPatents() instanceof List);
        assertEquals("title", account.getProfile().getPatents().get(0).getTitle());
        assertEquals("summary", account.getProfile().getPatents().get(0).getSummary());
        assertEquals("office", account.getProfile().getPatents().get(0).getOffice());
    }


}
