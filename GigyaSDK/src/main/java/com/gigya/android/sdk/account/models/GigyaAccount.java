package com.gigya.android.sdk.account.models;

import android.support.annotation.Nullable;

import com.gigya.android.sdk.network.GigyaResponseModel;
import com.gigya.android.sdk.session.SessionInfo;
import com.google.gson.annotations.SerializedName;

/**
 * Gigya main account model.
 * Model is an inline representation of the base scheme available in the client site.
 */
public class GigyaAccount extends GigyaResponseModel {

    @Nullable
    private String UID;
    @Nullable
    private String UIDSignature;
    @Nullable
    private Integer apiVersion;
    @Nullable
    private String created;
    @Nullable
    private Long createdTimestamp;
    @Nullable
    private Emails emails;
    private boolean isActive;
    private boolean isRegistered;
    private boolean isVerified;
    @Nullable
    private String lastLogin;
    @Nullable
    private Long lastLoginTimestamp;
    @Nullable
    private String lastUpdated;
    @Nullable
    private Long lastUpdatedTimestamp;
    @Nullable
    private String loginProvider;
    @Nullable
    private String oldestDataUpdated;
    @Nullable
    private Long oldestDataUpdatedTimestamp;
    @Nullable
    private Profile profile;
    @Nullable
    private String registered;
    @Nullable
    private Long registeredTimestamp;
    @Nullable
    private SessionInfo sessionInfo;
    @Nullable
    private Long signatureTimestamp;
    @Nullable
    private String socialProviders;
    @Nullable
    private String verified;
    @Nullable
    private Long verifiedTimestamp;

    @Nullable
    public String getUID() {
        return UID;
    }

    public void setUID(@Nullable String UID) {
        this.UID = UID;
    }

    @Nullable
    public String getUIDSignature() {
        return UIDSignature;
    }

    public void setUIDSignature(@Nullable String UIDSignature) {
        this.UIDSignature = UIDSignature;
    }

    @Nullable
    public Integer getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(@Nullable Integer apiVersion) {
        this.apiVersion = apiVersion;
    }

    @Nullable
    public String getCreated() {
        return created;
    }

    public void setCreated(@Nullable String created) {
        this.created = created;
    }

    @Nullable
    public Long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(@Nullable Long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    @Nullable
    public Emails getEmails() {
        return emails;
    }

    public void setEmails(@Nullable Emails emails) {
        this.emails = emails;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isRegistered() {
        return isRegistered;
    }

    public void setRegistered(boolean registered) {
        isRegistered = registered;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    @Nullable
    public String getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(@Nullable String lastLogin) {
        this.lastLogin = lastLogin;
    }

    @Nullable
    public Long getLastLoginTimestamp() {
        return lastLoginTimestamp;
    }

    public void setLastLoginTimestamp(@Nullable Long lastLoginTimestamp) {
        this.lastLoginTimestamp = lastLoginTimestamp;
    }

    @Nullable
    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(@Nullable String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Nullable
    public Long getLastUpdatedTimestamp() {
        return lastUpdatedTimestamp;
    }

    public void setLastUpdatedTimestamp(@Nullable Long lastUpdatedTimestamp) {
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
    }

    @Nullable
    public String getLoginProvider() {
        return loginProvider;
    }

    public void setLoginProvider(@Nullable String loginProvider) {
        this.loginProvider = loginProvider;
    }

    @Nullable
    public String getOldestDataUpdated() {
        return oldestDataUpdated;
    }

    public void setOldestDataUpdated(@Nullable String oldestDataUpdated) {
        this.oldestDataUpdated = oldestDataUpdated;
    }

    @Nullable
    public Long getOldestDataUpdatedTimestamp() {
        return oldestDataUpdatedTimestamp;
    }

    public void setOldestDataUpdatedTimestamp(@Nullable Long oldestDataUpdatedTimestamp) {
        this.oldestDataUpdatedTimestamp = oldestDataUpdatedTimestamp;
    }

    @Nullable
    public Profile getProfile() {
        return profile;
    }

    public void setProfile(@Nullable Profile profile) {
        this.profile = profile;
    }

    @Nullable
    public String getRegistered() {
        return registered;
    }

    public void setRegistered(@Nullable String registered) {
        this.registered = registered;
    }

    @Nullable
    public Long getRegisteredTimestamp() {
        return registeredTimestamp;
    }

    public void setRegisteredTimestamp(@Nullable Long registeredTimestamp) {
        this.registeredTimestamp = registeredTimestamp;
    }

    @Nullable
    public SessionInfo getSessionInfo() {
        return sessionInfo;
    }

    public void setSessionInfo(@Nullable SessionInfo sessionInfo) {
        this.sessionInfo = sessionInfo;
    }

    @Nullable
    public Long getSignatureTimestamp() {
        return signatureTimestamp;
    }

    public void setSignatureTimestamp(@Nullable Long signatureTimestamp) {
        this.signatureTimestamp = signatureTimestamp;
    }

    @Nullable
    public String getSocialProviders() {
        return socialProviders;
    }

    public void setSocialProviders(@Nullable String socialProviders) {
        this.socialProviders = socialProviders;
    }

    @Nullable
    public String getVerified() {
        return verified;
    }

    public void setVerified(@Nullable String verified) {
        this.verified = verified;
    }

    @Nullable
    public Long getVerifiedTimestamp() {
        return verifiedTimestamp;
    }

    public void setVerifiedTimestamp(@Nullable Long verifiedTimestamp) {
        this.verifiedTimestamp = verifiedTimestamp;
    }
}
