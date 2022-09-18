package com.gigya.android.sdk.session;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import android.util.Base64;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.collection.ArrayMap;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaInterceptor;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.encryption.EncryptionException;
import com.gigya.android.sdk.encryption.ISecureKey;
import com.gigya.android.sdk.encryption.SessionKey;
import com.gigya.android.sdk.encryption.SessionKeyLegacy;
import com.gigya.android.sdk.encryption.SessionKeyV2;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.persistence.PersistenceService;
import com.gigya.android.sdk.utils.CipherUtils;
import com.gigya.android.sdk.utils.ObjectUtils;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.Key;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class SessionService implements ISessionService {

    private static final String LOG_TAG = "SessionService";

    private boolean clearCookies = true;

    @Override
    public void setClearCookies(boolean clear) {
        clearCookies = clear;
    }

    // Final fields.
    final private Context _context;
    final private Config _config;
    final private IPersistenceService _psService;
    final private ISecureKey _secureKey;
    final private SessionStateHandler _observable;

    // Dynamic field - session heap.
    private SessionInfo _sessionInfo;

    // Injected field - session logic interceptors.
    private final ArrayMap<String, GigyaInterceptor> _sessionInterceptors = new ArrayMap<>();

    public SessionService(Context context,
                          Config config,
                          IPersistenceService psService,
                          ISecureKey secureKey,
                          SessionStateHandler observable) {
        _context = context;
        _psService = psService;
        _config = config;
        _secureKey = secureKey;
        _observable = observable;
    }

    /**
     * Fetch key used for session encryption/decryption.
     * Session key generation will vary according to OS level and SDK core version.
     *
     * @return SecretKey KeyStore generated secure key.
     */
    private SecretKey getKey(int optMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (SessionKeyV2.isUsed()) {
                return new SessionKeyV2().getKey();
            } else {
                switch (optMode) {
                    case Cipher.ENCRYPT_MODE:
                        return new SessionKeyV2().getKey();
                    case Cipher.DECRYPT_MODE:
                        return new SessionKey(_context, _psService).getKey();
                }
            }
            return null;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return new SessionKey(_context, _psService).getKey();
        } else {
            SessionKeyLegacy legacyKeyGenerator = new SessionKeyLegacy(_psService);
            return legacyKeyGenerator.getKey();
        }
    }

    @SuppressLint("GetInstance")
    @Nullable
    @Override
    public String encryptSession(String plain, Key key) throws EncryptionException {
        try {
            final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] ivSpec = cipher.getIV();
            _psService.add(PersistenceService.PREFS_KEY_IV_SPEC_SESSION, Base64.encodeToString(ivSpec, Base64.DEFAULT));
            byte[] byteCipherText = cipher.doFinal(plain.getBytes());
            return CipherUtils.bytesToString(byteCipherText);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new EncryptionException("encryptSession: exception" + ex.getMessage(), ex.getCause());
        }
    }


    @SuppressLint({"GetInstance"})
    @Nullable
    @Override
    public String decryptSession(String encrypted, Key key) throws EncryptionException {
        try {
            Cipher cipher;
            String ivSpecString = _psService.getString(PersistenceService.PREFS_KEY_IV_SPEC_SESSION, null);
            if (ivSpecString == null) {
                // Session encryption has not migrated to GCM.
                // Old session will be decrypted using "AES/ECB" nut will no longer use this algorithm.
                // New saved session encryption will be migrated with "AES/GCM/NoPadding".
                cipher = Cipher.getInstance("AES");
                GigyaLogger.debug(LOG_TAG, "ECB session decrypted");
                cipher.init(Cipher.DECRYPT_MODE, key);
            } else if (SessionKeyV2.isUsed() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec ivSpec = new GCMParameterSpec(128, Base64.decode(ivSpecString, Base64.DEFAULT));
                cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
            } else {
                cipher = Cipher.getInstance("AES/GCM/NoPadding");
                final IvParameterSpec iv = new IvParameterSpec(Base64.decode(ivSpecString, Base64.DEFAULT));
                GigyaLogger.debug(LOG_TAG, "GCM session decrypted");
                cipher.init(Cipher.DECRYPT_MODE, key, iv);
            }
            byte[] encPLBytes = CipherUtils.stringToBytes(encrypted);
            byte[] bytePlainText = cipher.doFinal(encPLBytes);
            return new String(bytePlainText);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new EncryptionException("decryptSession: exception" + ex.getMessage(), ex.getCause());
        }
    }

    /**
     * Persist session info using current encryption algorithm.
     *
     * @param sessionInfo Provided session.
     */
    @Override
    public void save(SessionInfo sessionInfo) {
        final String encryptionType = _psService.getSessionEncryptionType();
        if (!encryptionType.equals(GigyaDefinitions.SessionEncryption.DEFAULT)) {
            // Saving & encrypting the session via this service is only viable for "default" session encryption.
            return;
        }
        try {
            // Update persistence.
            final JSONObject jsonObject = new JSONObject()
                    .put("sessionToken", sessionInfo == null ? null : sessionInfo.getSessionToken())
                    .put("sessionSecret", sessionInfo == null ? null : sessionInfo.getSessionSecret())
                    .put("expirationTime", sessionInfo == null ? null : sessionInfo.getExpirationTime());
            final String json = jsonObject.toString();
            final SecretKey key = getKey(Cipher.ENCRYPT_MODE);
            final String encryptedSession = encryptSession(json, key);
            // Save session.
            _psService.setSession(encryptedSession);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Load current persistent session.
     */
    @Override
    public void load() {
        // Check & load legacy session if available.
        if (isLegacySession()) {
            GigyaLogger.debug(LOG_TAG, "load: isLegacySession!! Will migrate to update structure");
            _sessionInfo = loadLegacySession();
            return;
        }
        if (_psService.isSessionAvailable()) {
            String encryptedSession = _psService.getSession();
            if (!TextUtils.isEmpty(encryptedSession)) {
                final String encryptionType = _psService.getSessionEncryptionType();
                if (ObjectUtils.safeEquals(encryptionType, GigyaDefinitions.SessionEncryption.FINGERPRINT)) {
                    GigyaLogger.debug(LOG_TAG, "Fingerprint session available. Load stops until unlocked");
                }
                try {
                    final SecretKey key = getKey(Cipher.DECRYPT_MODE);
                    final String decryptedSession = decryptSession(encryptedSession, key);
                    Gson gson = new Gson();
                    // Parse session info.
                    final SessionInfo sessionInfo = gson.fromJson(decryptedSession, SessionInfo.class);

                    // Added in version 5.1.1.
                    migrateEncryptedDynamicConfig(decryptedSession, sessionInfo);

                    // Added in version 6.0.0
                    // Migrate session encryption to GCM.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (!SessionKeyV2.isUsed()) {
                            save(sessionInfo);
                        }
                    }
                    _sessionInfo = sessionInfo;
                } catch (Exception eex) {
                    eex.printStackTrace();
                }
            }
        }
    }

    /**
     * Added in version 5.1.1 to allow more secure id server rotation.
     * Removing config fields from session encryption if exist.
     * If so. Moving them to preferences.
     */
    private void migrateEncryptedDynamicConfig(String decryptedSession, SessionInfo sessionInfo) {
        try {
            JSONObject jo = new JSONObject(decryptedSession);
            if (!jo.has("gmid") || !jo.has("ucid")) return;
            final String gmid = jo.optString("gmid");
            if (!TextUtils.isEmpty(gmid)) {
                _psService.setGmid(gmid);
                _config.setGmid(gmid);
            }
            final String ucid = jo.optString("ucid");
            if (!TextUtils.isEmpty(ucid)) {
                _psService.setUcid(ucid);
                _config.setUcid(ucid);
            }
            // Re-save session.
            save(sessionInfo);
        } catch (JSONException e) {
            e.printStackTrace();
            GigyaLogger.error(LOG_TAG, "migrateEncryptedDynamicConfig failed");
        }
    }

    /**
     * Get current available session.
     *
     * @return Current session or null If none exist.
     */
    @Override
    public SessionInfo getSession() {
        return _sessionInfo;
    }

    /**
     * External session setter interface.
     * Will override the current session with given session info.
     * Session will be also persist.
     *
     * @param sessionInfo Provided session.
     */
    @Override
    public void setSession(SessionInfo sessionInfo) {
        GigyaLogger.debug(LOG_TAG, "setSession: ");
        _sessionInfo = sessionInfo;
        save(sessionInfo); // Will only work for "DEFAULT" encryption.
        // Apply interceptions
        applyInterceptions();

        // Check session expiration.
        if (_sessionInfo.getExpirationTime() > 0) {

            // Determine when the session will expire and persist it.
            long willExpireIn = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(_sessionInfo.getExpirationTime());
            _psService.setSessionExpiration(willExpireIn);

            // Start live countdown when the app is idle.
            startSessionCountdownTimerIfNeeded();
        }
    }

    /**
     * Check id current session validity.
     * Validity is evaluated via theses constraints:
     * #1 - Session object reference is not null.
     * #2 - Session contains token and secret.
     * #3 - If session contains expiration, check if not yet expired.
     *
     * @return True if session is valid.
     */
    @Override
    public boolean isValid() {
        boolean valid = _sessionInfo != null && _sessionInfo.isValid();
        final long willExpireIn = _psService.getSessionExpiration();
        if (valid && willExpireIn > 0) {
            valid = System.currentTimeMillis() < willExpireIn;
        }
        return valid;
    }

    /**
     * Clear session from memory.
     *
     * @param clearStorage Set True if session should be cleared from persistence as well.
     */
    @Override
    public void clear(boolean clearStorage) {
        GigyaLogger.debug(LOG_TAG, "clear: ");
        _sessionInfo = null;

        if (clearStorage) {
            // Remove session data. Update encryption to DEFAULT.
            _psService.removeSession();
            _psService.setSessionEncryptionType(GigyaDefinitions.SessionEncryption.DEFAULT);
        }
    }

    @Override
    public void clearCookiesOnLogout() {
        if (clearCookies) {
            CookieManager cookieManager = CookieManager.getInstance();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cookieManager.removeAllCookies(null);
                cookieManager.flush();
            } else {
                CookieSyncManager.createInstance(_context);
                cookieManager.removeAllCookie();
            }
        }
    }

    @Override
    public void registerExpirationObserver(SessionStateObserver observer) {
        _observable.registerExpirationObserver(observer);
    }

    @Override
    public void removeExpirationObserver(SessionStateObserver observer) {
        _observable.removeExpirationObserver(observer);
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

    //region LEGACY SESSION (Obsolete)

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
        _psService.removeLegacySession();
        // Save session in current construct.
        save(sessionInfo);
        return sessionInfo;
    }

    //endregion

    //region SESSION EXPIRATION

    private CountDownTimer _sessionLifeCountdownTimer;

    /**
     * Cancel running timer if reference is not null.
     */
    @Override
    public void cancelSessionCountdownTimer() {
        if (_sessionLifeCountdownTimer != null) {
            _sessionLifeCountdownTimer.cancel();
            _sessionLifeCountdownTimer = null;
        }
    }

    /**
     * Add custom session interception.
     * Interception will apply when you set a new session using setSession {@link #setSession}
     *
     * @param interceptor Provided interceptor implementation.
     */
    @Override
    public void addInterceptor(GigyaInterceptor interceptor) {
        _sessionInterceptors.put(interceptor.getName(), interceptor);
    }

    /**
     * Refresh the current session expiration timestamp.
     * For internal use.
     */
    @Override
    public void refreshSessionExpiration() {
        GigyaLogger.debug(LOG_TAG, "refreshSessionExpiration: ");
        // Get session expiration if exists.
        final long willExpireIn = _psService.getSessionExpiration();
        // Check if already passed. Reset if so.
        if (willExpireIn > 0 && willExpireIn < System.currentTimeMillis()) {

            // Session was set to expire. Time has passed. Session needs to be invalidated.
            _psService.setSessionExpiration(0);

            GigyaLogger.debug(LOG_TAG, "refreshSessionExpiration: Session expired. Clearing session");
            // Clear the session from heap & persistence.
            clear(true);
        } else if (willExpireIn > 0) {
            // Will start session countdown timer if the current session contains an expiration time.
            startSessionCountdownTimerIfNeeded();
        }
    }

    /**
     * Check if session countdown is required. Initiate if needed.
     */
    @Override
    public void startSessionCountdownTimerIfNeeded() {
        GigyaLogger.debug(LOG_TAG, "startSessionCountdownTimerIfNeeded: ");
        long now = System.currentTimeMillis();
        long willExpireIn = _psService.getSessionExpiration();
        long delta = willExpireIn - now;

        if (_sessionInfo == null) {
            return;
        }
        if (_sessionInfo.isValid() && willExpireIn > 0) {
            // Trigger session expiration countdown timer.
            startSessionCountdown(delta);
        }
    }

    /**
     * Initiate session expiration countdown.
     * When finished. A local broadcast will be triggered.
     *
     * @param future Number of milliseconds to count down.
     */
    private void startSessionCountdown(long future) {
        GigyaLogger.debug(LOG_TAG, "startSessionCountdown: Session is set to expire in: "
                + TimeUnit.MILLISECONDS.toSeconds(future) + " seconds");

        // Cancel timer.
        cancelSessionCountdownTimer();
        _sessionLifeCountdownTimer = new CountDownTimer(future, TimeUnit.SECONDS.toMillis(1)) {
            @Override
            public void onTick(long millisUntilFinished) {
                // KEEP THIS LOG COMMENTED TO AVOID SPAMMING LOGCAT!!!!!
                //GigyaLogger.debug(LOG_TAG, "startSessionCountdown: " + TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) + " seconds remaining until session will expire");
            }

            @Override
            public void onFinish() {
                _psService.setSessionExpiration(0);
                if (_sessionInfo != null && !_sessionInfo.isValid()) {
                    return;
                }
                GigyaLogger.debug(LOG_TAG, "startSessionCountdown: Session expiration countdown done! Session is invalid");
                // Clear the session from heap & persistence.
                clear(true);
                // Send "session expired" local broadcast.
                LocalBroadcastManager.getInstance(_context).sendBroadcast(new Intent(GigyaDefinitions.Broadcasts.INTENT_ACTION_SESSION_EXPIRED));
                // Notify session expiration observers.
                _observable.notifySessionExpired();
            }
        }.start();
    }

    //endregion
}
