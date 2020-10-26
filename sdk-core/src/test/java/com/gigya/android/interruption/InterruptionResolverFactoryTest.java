package com.gigya.android.interruption;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.interruption.IPendingRegistrationResolver;
import com.gigya.android.sdk.interruption.InterruptionResolverFactory;
import com.gigya.android.sdk.interruption.link.ILinkAccountsResolver;
import com.gigya.android.sdk.network.GigyaError;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
public class InterruptionResolverFactoryTest {

    @Mock
    GigyaApiResponse _mockResponse;

    @Mock
    IBusinessApiService _businessApiService;

    IoCContainer _container = new IoCContainer();

    InterruptionResolverFactory _resolverFactory;

    @Before
    public void setup() {
        _container.bind(IBusinessApiService.class, _businessApiService);
        _resolverFactory = new InterruptionResolverFactory(_container);
    }

    @Test
    public void test_interruption_for_pending_verification() {
        // Arrange
        when(_mockResponse.getErrorCode()).thenReturn(GigyaError.Codes.ERROR_ACCOUNT_PENDING_VERIFICATION);
        when(_mockResponse.getField("regToken", String.class)).thenReturn("mockRegToken");

        final GigyaLoginCallback callback = new GigyaLoginCallback() {
            @Override
            public void onSuccess(Object obj) {
                // Stub.
            }

            @Override
            public void onError(GigyaError error) {
                // Stub.
            }

            @Override
            public void onPendingVerification(@NonNull GigyaApiResponse response, @Nullable String regToken) {
                // Assert
                assertEquals("mockRegToken", regToken);
            }
        };

        // Act
        _resolverFactory.resolve(_mockResponse, callback);
    }

    @Test
    public void test_interruption_for_pending_registration() {
        // Arrange
        when(_mockResponse.getErrorCode()).thenReturn(GigyaError.Codes.ERROR_ACCOUNT_PENDING_REGISTRATION);

        final GigyaLoginCallback callback = new GigyaLoginCallback() {
            @Override
            public void onSuccess(Object obj) {
                // Stub.
            }

            @Override
            public void onError(GigyaError error) {
                // Stub.
            }

            @Override
            public void onPendingRegistration(@NonNull GigyaApiResponse response, @NonNull IPendingRegistrationResolver resolver) {
                assertNotNull(resolver);
            }
        };

        // Act
        _resolverFactory.resolve(_mockResponse, callback);
    }

    @Test
    public void test_interruption_for_pending_password_change() {
        // Arrange
        when(_mockResponse.getErrorCode()).thenReturn(GigyaError.Codes.ERROR_PENDING_PASSWORD_CHANGE);

        final GigyaLoginCallback callback = new GigyaLoginCallback() {
            @Override
            public void onSuccess(Object obj) {
                // Stub.
            }

            @Override
            public void onError(GigyaError error) {
                // Stub.
            }

            @Override
            public void onPendingPasswordChange(@NonNull GigyaApiResponse response) {
                assertEquals(response, _mockResponse);
            }
        };

        // Act
        _resolverFactory.resolve(_mockResponse, callback);
    }

    @Test
    public void test_interruption_for_login_identifier_exists() throws Exception {
        // Arrange
        when(_mockResponse.getErrorCode()).thenReturn(GigyaError.Codes.ERROR_LOGIN_IDENTIFIER_EXISTS);
        when(_mockResponse.getField("regToken", String.class)).thenReturn("mockRegToken");
        final GigyaLoginCallback callback = new GigyaLoginCallback() {
            @Override
            public void onSuccess(Object obj) {
                // Stub.
            }

            @Override
            public void onError(GigyaError error) {
                // Stub.
            }

            @Override
            public void onConflictingAccounts(@NonNull GigyaApiResponse response, @NonNull ILinkAccountsResolver resolver) {
                assertEquals(_mockResponse, response);
            }
        };

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                callback.onConflictingAccounts(_mockResponse, null);
                return null;
            }
        }).
        when(_businessApiService).getConflictingAccounts(anyString(), any(GigyaCallback.class));

        // Act
        _resolverFactory.resolve(_mockResponse, callback);
    }
}


