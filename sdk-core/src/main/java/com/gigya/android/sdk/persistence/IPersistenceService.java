package com.gigya.android.sdk.persistence;

import androidx.annotation.Nullable;

import com.gigya.android.sdk.GigyaDefinitions;

import java.util.List;
import java.util.Set;

public interface IPersistenceService {

    boolean isSessionAvailable();

    void setSession(String encryptedSession);

    String getSession();

    void setSessionExpiration(long expiration);

    long getSessionExpiration();

    void removeSession();

    void removeLegacySession();

    void setSessionEncryptionType(String encryptionType);

    String getSessionEncryptionType();

    Set<String> getSocialProviders();

    void addSocialProvider(@GigyaDefinitions.Providers.SocialProvider String provider);

    void removeSocialProviders();

    String getString(String key, String defValue);

    void add(String key, Object element);

    Long getLong(String key, Long defValue);

    void setPushToken(String pushToken);

    @Nullable
    String getPushToken();

    void setGmid(String gmid);

    void setUcid(String ucid);

    String getGmid();

    String getUcid();

    void setCoreVersion(String version);

    String getCoreVersion();

    void saveKeyHandles(String keyHandles);

    String getKeyHandles();
}
