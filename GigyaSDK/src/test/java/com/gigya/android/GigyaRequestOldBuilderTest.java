package com.gigya.android;

import android.util.Base64;

import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.model.SessionInfo;
import com.gigya.android.sdk.network.GigyaRequestBuilderOld;
import com.gigya.android.sdk.utils.UrlUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GigyaRequestBuilderOld.class, Base64.class})
@PowerMockIgnore("javax.crypto.*")
public class GigyaRequestOldBuilderTest {

    @Mock
    private SessionManager sessionManager;

    private SessionInfo sessionInfo;

    private TreeMap<String, Object> serverParams = new TreeMap<>();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        sessionInfo = new SessionInfo("asda34asfasfj9fuas", "mockSessionToken", 9223372036854775807L);
        when(sessionManager.getSession()).thenReturn(sessionInfo);

        serverParams = new TreeMap<>();
        serverParams.put("ApiKey", "mockApiKey");

        PowerMockito.mockStatic(Base64.class);
        PowerMockito.when(Base64.decode(anyString(), anyInt())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                return java.util.Base64.getMimeDecoder().decode((String) invocation.getArguments()[0]);
            }
        });
        PowerMockito.when(Base64.encode(any(byte[].class), anyInt())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                return java.util.Base64.getEncoder().encode((byte[]) invocation.getArguments()[0]);
            }
        });
        PowerMockito.when(Base64.encodeToString(any(byte[].class), anyInt())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                return new String(java.util.Base64.getEncoder().encode((byte[]) invocation.getArguments()[0]));
            }
        });
    }

    // TODO: 31/12/2018 Main issue is to test the build() method. Mock Volley.

    // TODO: 31/12/2018 Migrate URL relevant methods to URL Utils class.

    @Test
    public void testGetBaseUrl() throws Exception {
        Configuration configuration = new Configuration("mockApiKey", "us1.gigya.com");
        GigyaRequestBuilderOld builder = new GigyaRequestBuilderOld(configuration).api("socialize.getSdkConfig");

        final String baseUrl = Whitebox.invokeMethod(builder, "getBaseUrl");
        System.out.println(baseUrl);
        assertEquals(baseUrl, "https://socialize.us1.gigya.com/socialize.getSdkConfig");
    }

    @Test
    public void testGetUrl() throws Exception {
        Configuration configuration = new Configuration("mockApiKey", "us1.gigya.com");
        GigyaRequestBuilderOld builder = new GigyaRequestBuilderOld(configuration).api("socialize.getSdkConfig").httpMethod(0);

        final String encodedParams = UrlUtils.buildEncodedQuery(serverParams);
        final String url = Whitebox.invokeMethod(builder, "getUrl", encodedParams);
        System.out.println(url);
        assertEquals(url, "https://socialize.us1.gigya.com/socialize.getSdkConfig?ApiKey=mockApiKey");
    }

    @Test
    public void testAddAuthenticationParameters() throws Exception {
        Configuration configuration = new Configuration("mockApiKey", "us1.gigya.com");
        GigyaRequestBuilderOld builder = new GigyaRequestBuilderOld(configuration).api("socialize.getSdkConfig").httpMethod(0).sessionManager(sessionManager);
        FieldSetter.setField(builder, GigyaRequestBuilderOld.class.getDeclaredField("serverParams"), serverParams);

        Whitebox.invokeMethod(builder, "addAuthenticationParameters");
        System.out.println(serverParams.toString());
        assertNotNull(serverParams.get("nonce"));
        assertNotNull(serverParams.get("timestamp"));
        assertNotNull(serverParams.get("sig"));
    }
}
