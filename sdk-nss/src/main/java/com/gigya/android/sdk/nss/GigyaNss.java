package com.gigya.android.sdk.nss;

import android.annotation.SuppressLint;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.nss.bloc.SchemaHelper;
import com.gigya.android.sdk.nss.bloc.action.NssActionFactory;
import com.gigya.android.sdk.nss.bloc.action.NssForgotPasswordAction;
import com.gigya.android.sdk.nss.bloc.action.NssLinkAccountAction;
import com.gigya.android.sdk.nss.bloc.action.NssLoginAction;
import com.gigya.android.sdk.nss.bloc.action.NssOtpAction;
import com.gigya.android.sdk.nss.bloc.action.NssRegistrationAction;
import com.gigya.android.sdk.nss.bloc.action.NssSetAccountAction;
import com.gigya.android.sdk.nss.bloc.data.NssDataResolver;
import com.gigya.android.sdk.nss.bloc.data.NssJsEvaluator;
import com.gigya.android.sdk.nss.bloc.events.ScreenEventsManager;
import com.gigya.android.sdk.nss.bloc.flow.NssFlowManager;
import com.gigya.android.sdk.nss.channel.ApiMethodChannel;
import com.gigya.android.sdk.nss.channel.DataMethodChannel;
import com.gigya.android.sdk.nss.channel.EventsMethodChannel;
import com.gigya.android.sdk.nss.channel.IgnitionMethodChannel;
import com.gigya.android.sdk.nss.channel.LogMethodChannel;
import com.gigya.android.sdk.nss.channel.ScreenMethodChannel;
import com.gigya.android.sdk.nss.engine.NssEngineLifeCycle;

import java.lang.reflect.InvocationTargetException;

public class GigyaNss {

    public static final String VERSION = "1.9.10";

    private static final String LOG_TAG = "GigyaNss";

    @SuppressLint("StaticFieldLeak")
    private static GigyaNss _sharedInstance;

    public static synchronized GigyaNss getInstance() {
        if (_sharedInstance == null) {
            IoCContainer container = Gigya.getContainer();

            container.bind(GigyaNss.class, GigyaNss.class, true);
            container.bind(NssMarkupLoader.class, NssMarkupLoader.class, false);
            container.bind(NssEngineLifeCycle.class, NssEngineLifeCycle.class, false);
            container.bind(IgnitionMethodChannel.class, IgnitionMethodChannel.class, true);
            container.bind(ApiMethodChannel.class, ApiMethodChannel.class, true);
            container.bind(EventsMethodChannel.class, EventsMethodChannel.class, true);
            container.bind(DataMethodChannel.class, DataMethodChannel.class, true);
            container.bind(ScreenMethodChannel.class, ScreenMethodChannel.class, true);
            container.bind(LogMethodChannel.class, LogMethodChannel.class, true);
            container.bind(ScreenEventsManager.class, ScreenEventsManager.class, true);
            container.bind(NssFlowManager.class, NssFlowManager.class, false);
            container.bind(NssJsEvaluator.class, NssJsEvaluator.class, false);
            container.bind(NssRegistrationAction.class, NssRegistrationAction.class, false);
            container.bind(NssLoginAction.class, NssLoginAction.class, false);
            container.bind(NssSetAccountAction.class, NssSetAccountAction.class, false);
            container.bind(NssOtpAction.class, NssOtpAction.class, false);
            container.bind(NssActionFactory.class, NssActionFactory.class, false);
            container.bind(NssForgotPasswordAction.class, NssForgotPasswordAction.class, false);
            container.bind(NssLinkAccountAction.class, NssLinkAccountAction.class, false);
            container.bind(NssViewModel.class, NssViewModel.class, true);
            container.bind(SchemaHelper.class, SchemaHelper.class, false);
            container.bind(NssDataResolver.class, NssDataResolver.class, true);

            try {
                _sharedInstance = container.get(GigyaNss.class);
                GigyaLogger.debug(LOG_TAG, "Instantiation version: " + VERSION);
            } catch (Exception e) {
                GigyaLogger.error(LOG_TAG, "Error creating Gigya TFA library (did you forget to Gigya.setApplication?");
                e.printStackTrace();
                throw new RuntimeException("Error creating Gigya TFA library (did you forget to Gigya.setApplication?");
            }
        }
        return _sharedInstance;
    }

    private final String[] SUPPORTED_DEVICE_ARCHITECTURES = {"armv7l", "aarch64", "arm64-v8a", "armeabi-v7a"};

    /**
     * The native screen-sets engine supports only "ARM" architectures as a direct result of using the Flutter framework.
     * This method will check and verify that the feature is available for this specific device.
     * Do not use this method for testing on x86 emulator instances.
     */
    public boolean isSupported() {
        final String arch = System.getProperty("os.arch");
        if (arch != null) {
            for (String supported_device_architecture : SUPPORTED_DEVICE_ARCHITECTURES) {
                if (supported_device_architecture.equalsIgnoreCase(arch)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Load markup JSON file from assets folder.
     *
     * @param withAsset Asset JSON file name. Do not add .JSON prefix.
     * @return Nss.Builder instance. Use builder response to continue to flow.
     */
    public Nss.Builder loadFromAssets(String withAsset) {
        if (withAsset.endsWith(".json")) {
            withAsset = withAsset.substring(0, withAsset.length() - 5);
            GigyaLogger.debug(LOG_TAG, "loadFromAssets " + withAsset);
        }
        return new Nss.Builder().assetPath(withAsset);
    }

    /**
     * Load markup JSON file from assets folder.
     *
     * @param screenSetId Id of the hosted screen set.
     * @return Nss.Builder instance. Use builder response to continue to flow.
     */
    public Nss.Builder load(String screenSetId) {
        GigyaLogger.debug(LOG_TAG, "load " + screenSetId);
        return new Nss.Builder().screenSetId(screenSetId);
    }

    /**
     * Manually dismiss the NSS engine.
     */
    public void nssDismiss() {
        try {
            final NssViewModel model = Gigya.getContainer().get(NssViewModel.class);
            model.nssDismiss();
        } catch (Exception ex) {
            GigyaLogger.error(LOG_TAG, "Unable to get view model");
        }
    }
}
