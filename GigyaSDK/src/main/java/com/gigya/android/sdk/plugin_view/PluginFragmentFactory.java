package com.gigya.android.sdk.plugin_view;


public class PluginFragmentFactory implements IPluginFragmentFactory {

    final private IWebBridgeFactory _wbFactory;

    PluginFragmentFactory(IWebBridgeFactory wbFactory) {
        _wbFactory = wbFactory;
    }

    @Override
    public void showFragment(boolean obfuscate) {
        final PluginFragment fragment = new PluginFragment();
        fragment.inject(obfuscate, _wbFactory);
    }
}
