package com.gigya.android.sdk.auth.passkeys

import java.util.concurrent.CompletableFuture

interface IPasskeysAuthenticationProvider {

    fun createPasskey(requestJson: String): CompletableFuture<String?>

    fun getPasskey(requestJson: String): CompletableFuture<String?>

}