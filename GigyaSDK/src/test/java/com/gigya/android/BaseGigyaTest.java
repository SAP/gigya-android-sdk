package com.gigya.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.IoCContainer;
import com.gigya.android.sdk.account.AccountService;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.model.account.SessionInfo;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.persistence.PersistenceService;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.session.SessionService;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

public class BaseGigyaTest {

    @Mock
    Context mContext;

    @Mock
    Config mConfig;

    @Mock
    SessionService mSessionService;

    @Mock
    AccountService mAccountService;

    @Mock
    PersistenceService mPersistenceService;

    protected IoCContainer container;

    //region SHARED PREFERENCES

    @Mock
    protected SharedPreferences mSharedPreferences;

    @Mock
    protected SharedPreferences.Editor mEditor;

    //endregion

    @Before
    public void setup() throws Exception {
        container = new IoCContainer();
        container.bind(Context.class, mContext);
        container.bind(Config.class, mConfig);
        container.bind(ISessionService.class, mSessionService);
        container.bind(IAccountService.class, mAccountService);
        container.bind(IPersistenceService.class, mPersistenceService);
    }

    protected void mockApiDomain(String domain) {
        when(mConfig.getApiDomain()).thenReturn(domain);
    }

    protected void mockSession() {
        final SessionInfo sessionInfo = new SessionInfo(StaticMockFactory.getMockSecret(), StaticMockFactory.getMockToken());
        when(mSessionService.getSession()).thenReturn(sessionInfo);
    }

    protected void mockSessionWithExpiration(long expiration) {
        final SessionInfo sessionInfo = new SessionInfo(StaticMockFactory.getMockSecret(), StaticMockFactory.getMockToken(), expiration);
        when(mSessionService.getSession()).thenReturn(sessionInfo);
    }

    protected void mockLegacySession() {

    }

    //region HELPER CLASSES

    protected static class TestAccount extends GigyaAccount { }

    protected static class TestGigya extends Gigya<TestAccount> {

        TestGigya(@NonNull Context context, Class<TestAccount> accountScheme, IoCContainer container) {
            super(context, accountScheme, container);
        }
    }

    //endregion

    //region MOCK CONFIG

    void mockConfig() {
        when(mConfig.getApiKey()).thenReturn(StaticMockFactory.API_KEY);
        when(mConfig.getApiDomain()).thenReturn(StaticMockFactory.API_DOMAIN);
        when(mConfig.getGmid()).thenReturn(StaticMockFactory.GMID);
        when(mConfig.getUcid()).thenReturn(StaticMockFactory.UCID);
        // Mock update.
        when(mConfig.updateWith((Config) any())).thenReturn(mConfig);
    }

    //endregion

    //region MOCK SHARED PREFERENCES

    void mockSharedPreferences() {
        when(mContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mSharedPreferences);
        when(mSharedPreferences.edit()).thenReturn(mEditor);
        when(mEditor.putString(anyString(), anyString())).thenReturn(mEditor);
        when(mEditor.remove(anyString())).thenReturn(mEditor);
        doNothing().when(mEditor).apply();
    }

    //endregion

    //region STATIC MOCKS

    void mockAndroidTextUtils() {
        mockStatic(TextUtils.class);
        when(TextUtils.isEmpty((CharSequence) any())).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) {
                String s = (String) invocation.getArguments()[0];
                return s == null || s.length() == 0;
            }
        });
    }

    //endregion
}
