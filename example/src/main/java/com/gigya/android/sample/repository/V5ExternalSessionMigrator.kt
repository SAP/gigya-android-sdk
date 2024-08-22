package com.gigya.android.sample.repository

import android.annotation.SuppressLint
import android.content.Context
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.session.SessionInfo
import com.gigya.android.sdk.utils.CipherUtils
import com.google.gson.Gson
import java.security.KeyStore
import java.security.PrivateKey
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * V5ExternalSessionMigrator class is used to migrate deprecated session used until v5.1.7.
 * The migrator will try to decrypt the saved session and will re-encrypt is using the updated SDK code.
 * Use this migrator class if you want to update your app directly to Android v7 if you have skipped v6.
 *
 * Usage:
 * Create a new instance of the migrator file AFTER you have initialized the Gigya instance.
 * use the migrateV5Session method to start migration.
 */
class V5ExternalSessionMigrator(val context: Context) {

    private lateinit var keyStore: KeyStore

    companion object {
        const val PREF_FILE = "GSLIB"
        const val DEP_PREF_KEY = "GS_PREFA"
        const val DEP_ALIAS_KEY = "GS_ALIAS"
        const val DEP_SESSION_KEY = "GS_PREFS"
        const val DEP_KEY_TRANSFORMATION = "RSA/ECB/PKCS1Padding"
        const val DEP_TRANSFORMATION = "AES"
    }

    fun migrateV5Session(success: () -> Unit, error: () -> Unit) {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            if (keyStore.containsAlias(DEP_ALIAS_KEY)) {
                // Deprecated session exists and is required to be migrated.
                if (migrate()) success()
                return
            }
            error()
        } catch (ex: Exception) {
            ex.printStackTrace()
            error()
        }
    }

    @SuppressLint("GetInstance")
    private fun migrate(): Boolean {
        val sharedPreferences = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
        val aesKey: String = sharedPreferences.getString(DEP_PREF_KEY, null) ?: return false

        // Fetch deprecated key.
        val privateKey = keyStore.getKey(DEP_ALIAS_KEY, null) as PrivateKey
        val keyCipher: Cipher = Cipher.getInstance(DEP_KEY_TRANSFORMATION) ?: return false
        keyCipher.init(Cipher.DECRYPT_MODE, privateKey)
        val decrypted = keyCipher.doFinal(CipherUtils.stringToBytes(aesKey))
        val secretKey = SecretKeySpec(decrypted, 0, decrypted.size, DEP_TRANSFORMATION)

        // Decrypt deprecated session for re-encryption.
        val decryptionCipher = Cipher.getInstance(DEP_TRANSFORMATION)
        decryptionCipher.init(Cipher.DECRYPT_MODE, secretKey)
        val encrypted = sharedPreferences.getString(DEP_SESSION_KEY, null) ?: return false
        val encPLBytes = CipherUtils.stringToBytes(encrypted)
        val bytePlainText: ByteArray = decryptionCipher.doFinal(encPLBytes)
        val sessionString = String(bytePlainText)

        // Re-encrypt session.
        val sessionInfo: SessionInfo = Gson().fromJson(sessionString, SessionInfo::class.java)
        Gigya.getInstance().setSession(sessionInfo)

        // Delete deprecated entry.
        keyStore.deleteEntry("GS_ALIAS")
        return true
    }

}