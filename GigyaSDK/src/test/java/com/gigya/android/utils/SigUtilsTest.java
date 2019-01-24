package com.gigya.android.utils;

import android.util.Base64;

import com.gigya.android.sdk.utils.SigUtils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.TreeMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Base64.class})
@PowerMockIgnore("javax.crypto.*")
public class SigUtilsTest {

    private final String MOCK_SECRET = "asda34asfasfj9fuas";
    private final String MOCK_HTTP_METHOD = "POST";
    private final String MOCK_URL = "https://sociallize.us1.gigya.com/socialize.getSDKConfig";
    private final String MOCK_API_KEY = "asdkjasd83rffhsf8923rf2";
    private final TreeMap<String, Object> MOCK_PARAMETERS = new TreeMap<>();

    /*
    Test signature created with the above values.
     */
    private final String MOCK_SIGNATURE_VERIFIED = "fe4zimftu84s42OaQInV/RM+XJo=";

    @Before
    public void setup() {
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


    @Test
    public void testMatchingSignature() {
        // Arrange
        MOCK_PARAMETERS.put("ApiKey", MOCK_API_KEY);
        // Act
        String signature = SigUtils.getSignature(MOCK_SECRET, MOCK_HTTP_METHOD, MOCK_URL, MOCK_PARAMETERS);
        // Assert
        Assert.assertEquals(signature, MOCK_SIGNATURE_VERIFIED);
    }

    @Test
    public void testNonMatchingSignature() {
        // Arrange
        MOCK_PARAMETERS.put("ApiKey", "absd34as9uafs!@315asasnfasd");
        // Act
        String signature = SigUtils.getSignature(MOCK_SECRET, MOCK_HTTP_METHOD, MOCK_URL, MOCK_PARAMETERS);
        // Assert
        Assert.assertNotEquals(signature, MOCK_SIGNATURE_VERIFIED);
    }

    @Test
    public void testInvalidSignatureCreationWithNullSecret() {
        // Arrange
        MOCK_PARAMETERS.put("ApiKey", MOCK_API_KEY);
        // Act
        String signature = SigUtils.getSignature(null, MOCK_HTTP_METHOD, MOCK_URL, MOCK_PARAMETERS);
        // Assert
        Assert.assertNull(signature);
    }

    @Test
    public void testInvalidSignatureCreationWithNullHttpMethod() {
        // Arrange
        MOCK_PARAMETERS.put("ApiKey", MOCK_API_KEY);
        // Act
        String signature = SigUtils.getSignature(MOCK_SECRET, null, MOCK_URL, MOCK_PARAMETERS);
        // Assert
        Assert.assertNull(signature);
    }

    @Test
    public void testInvalidSignatureCreationWithNullURL() {
        // Arrange
        MOCK_PARAMETERS.put("ApiKey", MOCK_API_KEY);
        // Act
        String signature = SigUtils.getSignature(MOCK_SECRET, MOCK_HTTP_METHOD, null, MOCK_PARAMETERS);
        // Assert
        Assert.assertNull(signature);
    }

    @Test
    public void testInvalidSignatureCreationWithNullParameters() {
        // Arrange
        MOCK_PARAMETERS.put("ApiKey", MOCK_API_KEY);
        // Act
        String signature = SigUtils.getSignature(MOCK_SECRET, MOCK_HTTP_METHOD, MOCK_URL, null);
        // Assert
        Assert.assertNull(signature);
    }
}
