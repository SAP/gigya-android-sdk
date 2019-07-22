package com.gigya.android.interruption;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.interruption.IPendingRegistrationResolver;
import com.gigya.android.sdk.interruption.InterruptionResolverFactory;
import com.gigya.android.sdk.network.GigyaError;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
}


