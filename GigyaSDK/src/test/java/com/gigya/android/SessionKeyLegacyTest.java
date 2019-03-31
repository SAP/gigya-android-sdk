package com.gigya.android;

import com.gigya.android.sdk.encryption.SessionKeyLegacy;
import com.gigya.android.sdk.services.PersistenceService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.security.KeyPairGenerator;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import static junit.framework.TestCase.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Cipher.class, SessionKeyLegacy.class, KeyPairGenerator.class})
@PowerMockIgnore("javax.crypto.*")
public class SessionKeyLegacyTest {

    @Mock
    PersistenceService mPService;

    private SessionKeyLegacy TEST_INSTANCE;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getKeyTestNew() {
        // Arrange
        when(mPService.getString("GS_PREFA", null)).thenReturn("String");
        // Act
        TEST_INSTANCE = new SessionKeyLegacy(mPService);
        final SecretKey key = TEST_INSTANCE.getKey();
        // Assert
        assertNotNull(key);
    }

    @Test
    public void getKeyTestExisting() {
        // Arrange
        when(mPService.getString("GS_PREFA", null)).thenReturn(null);
        doNothing().when(mPService).add(anyString(), anyString());
        // Act
        TEST_INSTANCE = new SessionKeyLegacy(mPService);
        final SecretKey key = TEST_INSTANCE.getKey();
        // Assert
        assertNotNull(key);
    }
}
