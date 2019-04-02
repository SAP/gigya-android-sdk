package com.gigya.android.sdk.managers;

import android.annotation.SuppressLint;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.encryption.EncryptionException;
import com.gigya.android.sdk.encryption.ISecureKey;
import com.gigya.android.sdk.model.GigyaInterceptor;
import com.gigya.android.sdk.model.account.SessionInfo;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.persistence.PersistenceService;
import com.gigya.android.sdk.services.Config;
import com.gigya.android.sdk.utils.CipherUtils;
import com.gigya.android.sdk.utils.ObjectUtils;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.security.Key;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

public class SessionService implements ISessionService {

    private static final String LOG_TAG = "SessionService";

    // Final fields.
    final private Config _config;
    final private IPersistenceService _psService;
    final private ISecureKey _secureKey;

    // Dynamic field - session heap.
    private SessionInfo _sessionInfo;

    // Injected field - session logic interceptors.
    private ArrayMap<String, GigyaInterceptor> _sessionInterceptors = new ArrayMap<>();

    public SessionService(Config config, IPersistenceService psService, ISecureKey secureKey) {
        _psService = psService;
        _config = config;
        _secureKey = secureKey;
    }

    public void addInterceptor(GigyaInterceptor interceptor) {
        _sessionInterceptors.put(interceptor.getName(), interceptor);
    }

    @SuppressLint("GetInstance")
    @Nullable
    @Override
    public String encryptSession(String plain, Key key) throws EncryptionException {
        try {
            final String ENCRYPTION_ALGORITHM = "AES";
            final Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] byteCipherText = cipher.doFinal(plain.getBytes());
            return CipherUtils.bytesToString(byteCipherText);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new EncryptionException("encryptSession: exception" + ex.getMessage(), ex.getCause());
        }
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
        final String encryptionType = _psService.getString(PersistenceService.PREFS_KEY_SESSION_ENCRYPTION_TYPE, "DEFAULT");
        if (!encryptionType.equals("DEFAULT")) {
            // Saving & encrypting the session via this service is only viable for "default" session encryption.
            return;
        }
        try {
            // Update persistence.
            final JSONObject jsonObject = new JSONObject()
                    .put("sessionToken", sessionInfo == null ? null : sessionInfo.getSessionToken())
                    .put("sessionSecret", sessionInfo == null ? null : sessionInfo.getSessionSecret())
                    .put("expirationTime", sessionInfo == null ? null : sessionInfo.getExpirationTime())
                    .put("ucid", _config.getUcid())
                    .put("gmid", _config.getGmid());
            final String json = jsonObject.toString();
            final SecretKey key = _secureKey.getKey();
            final String encryptedSession = encryptSession(json, key);
            // Save session.
            _psService.add(PersistenceService.PREFS_KEY_SESSION, encryptedSession);
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
        if (_psService.contains(PersistenceService.PREFS_KEY_SESSION)) {
            String encryptedSession = _psService.getString(PersistenceService.PREFS_KEY_SESSION, null);
            if (!TextUtils.isEmpty(encryptedSession)) {
                final String encryptionType = _psService.getString(PersistenceService.PREFS_KEY_SESSION_ENCRYPTION_TYPE, "DEFAULT");
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
                    _config.updateWith(dynamicConfig);
                    return sessionInfo;
                } catch (Exception eex) {
                    eex.printStackTrace();
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    public SessionInfo getSession() {
        return _sessionInfo;
    }

    @Override
    public void setSession(SessionInfo sessionInfo) {
        _sessionInfo = sessionInfo;
        save(sessionInfo); // Will only work for "DEFAULT" encryption.
        // Apply interceptions
        applyInterceptions();
    }

    @Override
    public boolean isValid() {
        boolean valid = _sessionInfo != null && _sessionInfo.isValid();
        if (_sessionWillExpireIn > 0) {
            valid = System.currentTimeMillis() < _sessionWillExpireIn;
        }
        return valid;
    }

    private void applyInterceptions() {
        if (_sessionInterceptors.isEmpty()) {
            return;
        }
        for (Map.Entry<String, GigyaInterceptor> entry : _sessionInterceptors.entrySet()) {
            final GigyaInterceptor interceptor = entry.getValue();
            GigyaLogger.debug(LOG_TAG, "Apply interception for: " + interceptor.getName());
            interceptor.intercept();
        }
    }

    //region LEGACY SESSION

    private boolean isLegacySession() {
        final String legacyTokenKey = "session.Token";
        return (!TextUtils.isEmpty(_psService.getString(legacyTokenKey, null)));
    }

    private SessionInfo loadLegacySession() {
        final String token = _psService.getString("session.Token", null);
        final String secret = _psService.getString("session.Secret", null);
        final long expiration = _psService.getLong("session.ExpirationTime", 0L);
        final SessionInfo sessionInfo = new SessionInfo(secret, token, expiration);
        // Update configuration fields.
        final String ucid = _psService.getString("ucid", null);
        final String gmid = _psService.getString("gmid", null);
        final Config dynamicConfig = new Config();
        dynamicConfig.setUcid(ucid);
        dynamicConfig.setGmid(gmid);
        _config.updateWith(dynamicConfig);
        // Clear all legacy session entries.
        _psService.remove("ucid", "gmid", "lastLoginProvider", "session.Token",
                "session.Secret", "tsOffset", "session.ExpirationTime");
        // Save session in current construct.
        save(sessionInfo);
        return sessionInfo;
    }

    //endregion

    //region SESSION EXPIRATION

    private long _sessionWillExpireIn = 0;

    //endregion
}
