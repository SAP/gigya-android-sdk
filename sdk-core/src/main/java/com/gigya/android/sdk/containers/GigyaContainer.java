package com.gigya.android.sdk.containers;

import android.os.Build;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.ConfigFactory;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.account.accountCacheService;
import com.gigya.android.sdk.api.ApiService;
import com.gigya.android.sdk.api.BusinessApiService;
import com.gigya.android.sdk.api.GigyaApiRequestFactory;
import com.gigya.android.sdk.api.IApiRequestFactory;
import com.gigya.android.sdk.api.IApiService;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.auth.FidoApiServiceImpl;
import com.gigya.android.sdk.auth.FidoApiServiceV23Impl;
import com.gigya.android.sdk.auth.IFidoApiService;
import com.gigya.android.sdk.auth.IOauthService;
import com.gigya.android.sdk.auth.ISaptchaService;
import com.gigya.android.sdk.auth.IWebAuthnService;
import com.gigya.android.sdk.auth.OauthService;
import com.gigya.android.sdk.auth.SaptchaService;
import com.gigya.android.sdk.auth.WebAuthnService;
import com.gigya.android.sdk.interruption.IInterruptionResolverFactory;
import com.gigya.android.sdk.interruption.InterruptionResolverFactory;
import com.gigya.android.sdk.network.adapter.IRestAdapter;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.persistence.PersistenceService;
import com.gigya.android.sdk.providers.IProviderFactory;
import com.gigya.android.sdk.providers.ProviderFactory;
import com.gigya.android.sdk.push.GigyaNotificationManager;
import com.gigya.android.sdk.push.IGigyaNotificationManager;
import com.gigya.android.sdk.reporting.IReportingManager;
import com.gigya.android.sdk.reporting.IReportingService;
import com.gigya.android.sdk.reporting.ReportingManager;
import com.gigya.android.sdk.reporting.ReportingService;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.session.ISessionVerificationService;
import com.gigya.android.sdk.session.SessionService;
import com.gigya.android.sdk.session.SessionStateHandler;
import com.gigya.android.sdk.session.SessionVerificationService;
import com.gigya.android.sdk.ui.IPresenter;
import com.gigya.android.sdk.ui.Presenter;
import com.gigya.android.sdk.ui.plugin.GigyaPluginFragment;
import com.gigya.android.sdk.ui.plugin.GigyaWebBridge;
import com.gigya.android.sdk.ui.plugin.IGigyaPluginFragment;
import com.gigya.android.sdk.ui.plugin.IGigyaWebBridge;
import com.gigya.android.sdk.ui.plugin.IWebViewFragmentFactory;
import com.gigya.android.sdk.ui.plugin.WebViewFragmentFactory;
import com.gigya.android.sdk.ui.plugin.webbridgetmanager.IWebBridgeInterruptionManager;
import com.gigya.android.sdk.ui.plugin.webbridgetmanager.WebBridgeInterruptionManager;
import com.gigya.android.sdk.utils.FileUtils;

public class GigyaContainer extends IoCContainer {
    public GigyaContainer() {
        bind(FileUtils.class, FileUtils.class, true)
                .bind(Config.class, Config.class, true)
                .bind(ConfigFactory.class, ConfigFactory.class, false)
                .bind(IRestAdapter.class, RestAdapter.class, true)
                .bind(IPersistenceService.class, PersistenceService.class, false)
                .bind(IApiService.class, ApiService.class, false)
                .bind(IReportingService.class, ReportingService.class, true)
                .bind(IReportingManager.class, ReportingManager.class, true)
                .bind(IApiRequestFactory.class, GigyaApiRequestFactory.class, true)
                .bind(SessionStateHandler.class, SessionStateHandler.class, true)
                .bind(ISessionService.class, SessionService.class, true)
                .bind(IAccountService.class, accountCacheService.class, true)
                .bind(ISessionVerificationService.class, SessionVerificationService.class, true)
                .bind(IProviderFactory.class, ProviderFactory.class, true)
                .bind(IBusinessApiService.class, BusinessApiService.class, true)
                .bind(IOauthService.class, OauthService.class, true)
                .bind(IFidoApiService.class, Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? FidoApiServiceV23Impl.class : FidoApiServiceImpl.class, true)
                .bind(IWebAuthnService.class, WebAuthnService.class, true)
                .bind(ISaptchaService.class, SaptchaService.class, false)
                .bind(IWebViewFragmentFactory.class, WebViewFragmentFactory.class, false)
                .bind(IPresenter.class, Presenter.class, false)
                .bind(IInterruptionResolverFactory.class, InterruptionResolverFactory.class, true)
                .bind(IGigyaPluginFragment.class, GigyaPluginFragment.class, false)
                .bind(IGigyaNotificationManager.class, GigyaNotificationManager.class, true)
                .bind(IWebBridgeInterruptionManager.class, WebBridgeInterruptionManager.class, true)
                .bind(IGigyaWebBridge.class, GigyaWebBridge.class, true)
                .bind(IoCContainer.class, this);
    }
}
