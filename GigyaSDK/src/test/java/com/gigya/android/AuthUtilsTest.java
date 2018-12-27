package com.gigya.android;

import android.util.Base64;

import com.gigya.android.sdk.utils.AuthUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Random;
import java.util.TreeMap;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Base64.class, System.class, Random.class, AuthUtils.class})
@PowerMockIgnore("javax.crypto.*")
public class AuthUtilsTest {

    private final String MOCK_URL = "https://sociallize.us1.gigya.com/socialize.getSDKConfig";

    @Mock
    private Random mockRandom;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
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

        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.currentTimeMillis()).thenReturn(1545905337000L);
        PowerMockito.whenNew(Random.class).withNoArguments().thenReturn(mockRandom);
        PowerMockito.when(mockRandom.nextInt()).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                return 1;
            }
        });
    }


    @Test
    public void testAddAuthenticationParameters() {
        final String sessionSecret = "asda34asfasfj9fuas";
        final int httpMethod = 1; //"POST"
        final TreeMap<String, Object> params = new TreeMap<String, Object>() {{
            put("ApiKey", "someApiKey");
        }};
        AuthUtils.addAuthenticationParameters(sessionSecret, httpMethod, MOCK_URL, params);
        System.out.println(params.toString());

        assertNotNull(params.get("timestamp"));
        final String timestamp = (String) params.get("timestamp");
        assertEquals(timestamp, String.valueOf(1545905337));

        assertNotNull(params.get("nonce"));
        final String nonce = (String) params.get("nonce");
        assertEquals(nonce, "1545905337000_1");

        assertNotNull(params.get("sig"));
        final String signature = (String) params.get("sig");
        assertEquals(signature, "nC69hzGbTdPW3WlUl6k0ZeCd0CY=");
    }

    // {ApiKey=someApiKey, nonce=1545905337000_1, sig=nC69hzGbTdPW3WlUl6k0ZeCd0CY=, timestamp=1545905337}
}
