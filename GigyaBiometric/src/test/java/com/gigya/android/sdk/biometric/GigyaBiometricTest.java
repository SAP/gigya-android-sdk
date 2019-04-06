package com.gigya.android.sdk.biometric;

import android.content.Context;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.model.account.GigyaAccount;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Gigya.class, GigyaBiometricUtils.class, PersistenceService.class})
public class GigyaBiometricTest {

    @Mock
    Context context;

    @Mock
    Gigya<GigyaAccount> gigya;

    @Mock
    SessionService sessionService;

    @Mock
    PersistenceService persistenceService;

    @Before
    public void setup() {
        PowerMockito.mockStatic(Gigya.class, GigyaBiometricUtils.class);
        when(Gigya.getInstance()).thenReturn(gigya);
        when(gigya.getContext()).thenReturn(context);
        when(gigya.getGigyaComponent(SessionService.class)).thenReturn(sessionService);
        when(sessionService.getPersistenceService()).thenReturn(persistenceService);
        when(GigyaBiometricUtils.isSupported(context)).thenReturn(true);
        when(GigyaBiometricUtils.isPromptEnabled()).thenReturn(true);
        when(GigyaBiometricUtils.hasEnrolledFingerprints(context)).thenReturn(true);
    }

    @Test
    public void testIsOptIn() {
        // Arrange.
        when(persistenceService.getSessionEncryption()).thenReturn(SessionService.FINGERPRINT);
        final GigyaBiometric biometric = GigyaBiometric.getInstance();
        // Act.
        boolean isOptIn = biometric.isOptIn();
        // Assert.
        assertTrue(isOptIn);
    }

    @Test
    public void testIsOptInWithDefault() {
        // Arrange
        when(persistenceService.getSessionEncryption()).thenReturn(SessionService.DEFAULT);
        final GigyaBiometric biometric = GigyaBiometric.getInstance();
        // Act
        boolean isOptIn = biometric.isOptIn();
        // Assert
        assertFalse(isOptIn);
    }

    @Test
    public void testLockedWithValidSession() {
        // Arrange
        when(sessionService.isValidSession()).thenReturn(true);
        when(persistenceService.getSessionEncryption()).thenReturn(SessionService.FINGERPRINT);
        final GigyaBiometric biometric = GigyaBiometric.getInstance();
        // Act
        boolean isLocked = biometric.isLocked();
        // Assert
        assertFalse(isLocked);
    }

    @Test
    public void testLockedWithInvalidSession() {
        // Arrange
        when(sessionService.isValidSession()).thenReturn(false);
        when(persistenceService.getSessionEncryption()).thenReturn(SessionService.FINGERPRINT);
        final GigyaBiometric biometric = GigyaBiometric.getInstance();
        // Act
        boolean isLocked = biometric.isLocked();
        // Assert
        assertTrue(isLocked);
    }
}
