package com.gigya.android.sdk.managers;

import android.annotation.SuppressLint;
import android.os.Build;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.IGigyaContext;
import com.gigya.android.sdk.encryption.EncryptionException;
import com.gigya.android.sdk.encryption.ISecureKey;
import com.gigya.android.sdk.encryption.SessionKey;
import com.gigya.android.sdk.encryption.SessionKeyLegacy;
import com.gigya.android.sdk.model.account.SessionInfo;
import com.gigya.android.sdk.services.Config;
import com.gigya.android.sdk.services.PersistenceService;
import com.gigya.android.sdk.utils.CipherUtils;
import com.gigya.android.sdk.utils.ObjectUtils;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

public class SessionService implements ISessionService {

    private static final String LOG_TAG = "SessionService";

    // Final fields.
    final private PersistenceService _pService;
    final private ISecureKey _secureKey;

    final private IGigyaContext _gigyaContext;

    public SessionService(IGigyaContext gigyaContext) {
        _gigyaContext = gigyaContext;
        _pService = new PersistenceService(gigyaContext.getContext());
        // Initialize relevant key.
        _secureKey = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 ?
                new SessionKey(gigyaContext.getContext(), _pService) : new SessionKeyLegacy(_pService);
    }

    @SuppressLint("GetInstance")
    @Nullable
    @Override
    public String encryptSession(String plain, Key key) throws EncryptionException {
        try {
            final String ENCRYPTION_ALGORITHM = "AES";
            final Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new EncryptionException("encryptSession: exception" + ex.getMessage(), ex.getCause());
        }
        return null;
    }

    @SuppressLint("GetInstance")
    @Nullable
    @Override
    public String decryptSession(String encrypted, Key key) throws EncryptionException {
        try {
            final String ENCRYPTION_ALGORITHM = "AES";
            final Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] encPLBytes = CipherUtils.stringToBytes(encrypted);
            byte[] bytePlainText = cipher.doFinal(encPLBytes);
            return new String(bytePlainText);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new EncryptionException("decryptSession: exception" + ex.getMessage(), ex.getCause());
        }
    }

    @Override
    public void save(SessionInfo sessionInfo) {
        if (sessionInfo != null && sessionInfo.isValid()) try {
            // Update persistence.
            final JSONObject jsonObject = new JSONObject()
                    .put("sessionToken", sessionInfo.getSessionToken())
                    .put("sessionSecret", sessionInfo.getSessionSecret())
                    .put("expirationTime", sessionInfo.getExpirationTime())
                    .put("ucid", _gigyaContext.getConfig().getUcid())
                    .put("gmid", _gigyaContext.getConfig().getGmid());
            final String json = jsonObject.toString();
            final SecretKey key = _secureKey.getKey();
            final String encryptedSession = encryptSession(json, key);
            // Save session.
            _pService.setSession(encryptedSession);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Nullable
    @Override
    public SessionInfo load() {
        // Check & load legacy session if available.
        if (isLegacySession()) {
            GigyaLogger.debug(LOG_TAG, "load: isLegacySession!! Will migrate to update structure");
            return loadLegacySession();
        }
        if (_pService.hasSession()) {
            String encryptedSession = _pService.getSession();
            if (!TextUtils.isEmpty(encryptedSession)) {
                final String encryptionType = _pService.getSessionEncryption();
                if (ObjectUtils.safeEquals(encryptionType, "FINGERPRINT")) {
                    GigyaLogger.debug(LOG_TAG, "Fingerprint session available. Load stops until unlocked");
                    return null;
                }
                try {
                    final SecretKey key = _secureKey.getKey();
                    final String decryptedSession = decryptSession(encryptedSession, key);
                    Gson gson = new Gson();
                    // Parse session info.
                    final SessionInfo sessionInfo = gson.fromJson(decryptedSession, SessionInfo.class);
                    // Parse config fields. & update main SDK config instance.
                    final Config dynamicConfig = gson.fromJson(decryptedSession, Config.class);
                    _gigyaContext.updateConfig(dynamicConfig);
                    // Check for V3 session key. Remove it if exists.
                    if (removeV3SecretKey()) {
                        // Save the session info again so encryption flow will update to v4.
                        save(sessionInfo);
                    }
                    return sessionInfo;
                } catch (Exception eex) {
                    eex.printStackTrace();
                    return null;
                }
            }
        }
        return null;
    }

    //region LEGACY SESSION

    private boolean removeV3SecretKey() {
        final String v3Key = _pService.getString("GS_PREFA", null);
        if (v3Key != null) {
            _pService.remove(v3Key);
            return true;
        }
        return false;
    }

    private boolean isLegacySession() {
        final String legacyTokenKey = "session.Token";
        return (!TextUtils.isEmpty(_pService.getString(legacyTokenKey, null)));
    }

    private SessionInfo loadLegacySession() {
        final String token = _pService.getString("session.Token", null);
        final String secret = _pService.getString("session.Secret", null);
        final long expiration = _pService.getLong("session.ExpirationTime", 0L);
        final SessionInfo sessionInfo = new SessionInfo(secret, token, expiration);
        // Update configuration fields.
        final String ucid = _pService.getString("ucid", null);
        final String gmid = _pService.getString("gmid", null);
        final Config dynamicConfig = new Config();
        dynamicConfig.setUcid(ucid);
        dynamicConfig.setGmid(gmid);
        _gigyaContext.updateConfig(dynamicConfig);
        // Clear all legacy session entries.
        _pService.remove("ucid", "gmid", "lastLoginProvider", "session.Token",
                "session.Secret", "tsOffset", "session.ExpirationTime");
        // Save session in current construct.
        save(sessionInfo);
        return sessionInfo;
    }

    //endregion
}
