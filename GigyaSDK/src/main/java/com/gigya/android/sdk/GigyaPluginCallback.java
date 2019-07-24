package com.gigya.android.sdk;

import android.support.annotation.NonNull;

import com.gigya.android.sdk.ui.plugin.GigyaPluginEvent;

/**
 * Plugin specific event callback.
 *
 * @param <A> Custom account type provided int the Gigya interface initialization. If non specified will use basic GigyaAccount type.
 */
public abstract class GigyaPluginCallback<A> {

    /* Called when an error occurs. */
    public void onError(GigyaPluginEvent event) {
        // Stub.
    }

    /* Called on user cancel initiated. */
    public void onCanceled() {
        // Stub.
    }

    /* Called before validation of the form. */
    public void onBeforeValidation(@NonNull GigyaPluginEvent event) {
        // Stub.
    }

    /*
    Called before a form is submitted. This event gives you an opportunity to perform certain actions before
    the form is submitted, or cancel the submission by returning false.
    */
    public void onBeforeSubmit(@NonNull GigyaPluginEvent event) {
        // Stub.
    }

    /*
    Called when a form is submitted, can return a value or a promise. This event gives you an opportunity
    to modify the form data when it is submitted.
    */
    public void onSubmit(@NonNull GigyaPluginEvent event) {
        // Stub.
    }

    /* Called after a form is submitted. */
    public void onAfterSubmit(@NonNull GigyaPluginEvent event) {
        // Stub.
    }

    /*
    Called before a new screen is rendered. This event gives you an opportunity to
    cancel the navigation by returning false.
    */
    public void onBeforeScreenLoad(@NonNull GigyaPluginEvent event) {
        // Stub.
    }

    /* Called after a new screen is rendered. */
    public void onAfterScreenLoad(@NonNull GigyaPluginEvent event) {
        // Stub.
    }

    /* Called when a field is changed in a managed form. */
    public void onFieldChanged(@NonNull GigyaPluginEvent event) {
        // Stub.
    }

    /* Called when a user clicks the "X" (close) button or the screen is hidden following the end of the flow. */
    public void onHide(@NonNull GigyaPluginEvent event, @GigyaDefinitions.Plugin.PluginReason String reason) {
        // Stub.
    }

    /* Called when an updated account instance is available during the flow. */
    public void onLogin(@NonNull A accountObj) {
        // Stub.
    }

    /* Called when a after a logout action occurs. */
    public void onLogout() {
        // Stub.
    }

    /* Called when a new connection is successfully added to the account. */
    public void onConnectionAdded() {
        // Stub.
    }

    /* Called when an existing connection is removed from the account., */
    public void onConnectionRemoved() {
        // Stub.
    }

}
