package com.gigya.android;

import android.content.Context;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.api.ApiService;
import com.gigya.android.sdk.api.BusinessApiService;
import com.gigya.android.sdk.api.IApiService;
import com.gigya.android.sdk.interruption.IInterruptionsHandler;
import com.gigya.android.sdk.model.account.SessionInfo;
import com.gigya.android.sdk.network.GigyaApiRequest;
import com.gigya.android.sdk.network.adapter.IRestAdapter;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.providers.IProviderFactory;
import com.gigya.android.sdk.session.ISessionService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
public class BusinessApiServiceTest {

    @Mock
    Context mContext;

    @Mock
    Config mConfig;

    @Mock
    IRestAdapter mRestAdapter;

    @Mock
    ISessionService mSessionService;

    @Mock
    SessionInfo mSessionInfo;

    @Mock
    IAccountService mAccountService;

    @Mock
    IPersistenceService mPersistenceService;

    @Mock
    IApiService mApiService;

    @Mock
    IProviderFactory mProviderFactory;

    @Mock
    IInterruptionsHandler mInterruptionHandler;

    @Mock
    ApiService.IApiServiceResponse mResponse;

    @InjectMocks
    BusinessApiService cBusinessApiService;

    @Before
    public void setup() {

    }

    @Test
    public void testLogout() {
        // Arrange.
        when(mSessionService.isValid()).thenReturn(true);
        when(mSessionService.getSession()).thenReturn(mSessionInfo);
        when(mSessionService.getSession().getSessionToken()).thenReturn(null);
        when(mConfig.getGmid()).thenReturn("mockGmid");
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) {
                return null;
            }
        }).when(mApiService).send((GigyaApiRequest) any(), anyBoolean(), (ApiService.IApiServiceResponse) any());

        cBusinessApiService.logout();
    }
}
