package com.gigya.android.sdk.auth.passkeys

import java.util.concurrent.CompletableFuture

interface IPasskeysAuthenticationProvider {

    fun createPasskey(requestJson: String): CompletableFuture<CreateCredentialResult?>

    fun getPasskey(requestJson: String): CompletableFuture<GetCredentialResult?>

}