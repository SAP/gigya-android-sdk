package com.gigya.android.sample.data

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
 * Migrates a persisted v5 SDK session (up to v5.1.7) to the current encryption scheme.
 *
 * The v5 SDK stored sessions using RSA/AES in the AndroidKeyStore under a fixed alias
 * ("GS_ALIAS"). From v6 onwards the SDK uses a different key and storage strategy.
 * This migrator decrypts the old session and re-encrypts it using the current SDK,
 * allowing users upgrading directly from v5 to v7+ to retain their session without
 * being forced to re-authenticate.
 *
 * **Call site:** instantiate and call [migrateV5Session] in [ExampleApplication.onCreate],
 * **after** [Gigya.setApplication] and [Gigya.getInstance] have been called. The Gigya
 * instance must be initialised before migration so the re-encrypted session can be stored.
 *
 * If no v5 session exists the [error] callback is invoked and the app continues normally —
 * this is the expected path for all users who were never on v5.
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

    /**
     * Attempts to find and migrate a v5 session.
     *
     * @param success Invoked when a v5 session was found and successfully migrated.
     * @param error Invoked when no v5 session exists, or migration fails. In both
     *   cases the app should continue normally — re-authentication will be required
     *   only if a valid current session is also absent.
     */
    fun migrateV5Session(success: () -> Unit, error: () -> Unit) {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            if (keyStore.containsAlias(DEP_ALIAS_KEY)) {
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

        val privateKey = keyStore.getKey(DEP_ALIAS_KEY, null) as PrivateKey
        val keyCipher: Cipher = Cipher.getInstance(DEP_KEY_TRANSFORMATION) ?: return false
        keyCipher.init(Cipher.DECRYPT_MODE, privateKey)
        val decrypted = keyCipher.doFinal(CipherUtils.stringToBytes(aesKey))
        val secretKey = SecretKeySpec(decrypted, 0, decrypted.size, DEP_TRANSFORMATION)

        val decryptionCipher = Cipher.getInstance(DEP_TRANSFORMATION)
        decryptionCipher.init(Cipher.DECRYPT_MODE, secretKey)
        val encrypted = sharedPreferences.getString(DEP_SESSION_KEY, null) ?: return false
        val encPLBytes = CipherUtils.stringToBytes(encrypted)
        val bytePlainText: ByteArray = decryptionCipher.doFinal(encPLBytes)
        val sessionString = String(bytePlainText)

        val sessionInfo: SessionInfo = Gson().fromJson(sessionString, SessionInfo::class.java)
        Gigya.getInstance().setSession(sessionInfo)

        // Remove the deprecated AndroidKeyStore entry after successful migration.
        keyStore.deleteEntry("GS_ALIAS")
        return true
    }
}
