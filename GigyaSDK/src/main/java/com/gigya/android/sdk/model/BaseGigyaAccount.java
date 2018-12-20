package com.gigya.android.sdk.model;

public class BaseGigyaAccount {

    private String UID;
    private String UIDSignature;
    private int apiVersion;
    private String created;
    private long createdTimestamp;
    private Emails emails;
    private boolean isActive;
    private boolean isRegistered;
    private boolean isVerified;
    private String lastLogin;
    private long lastLoginTimestamp;
    private String lastUpdated;
    private long lastUpdatedTimestamp;
    private String loginProvider;
    private String oldestDataUpdated;
    private long oldestDataUpdatedTimestamp;
    private Preferences preferences;
    private Profile profile;
    private String registered;
    private long registeredTimestamp;
    private SessionInfo sessionInfo;
    private long signatureTimestamp;
    private String socialProviders;
    private String verified;
    private long verifiedTimestamp;
    private String regToken;

    //region Setters & Getters

    public String getUID() {
        return UID;
    }

    public void setUID(String UID) {
        this.UID = UID;
    }

    public String getUIDSignature() {
        return UIDSignature;
    }

    public void setUIDSignature(String UIDSignature) {
        this.UIDSignature = UIDSignature;
    }

    public int getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(int apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public Emails getEmails() {
        return emails;
    }

    public void setEmails(Emails emails) {
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

    public String getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(String lastLogin) {
        this.lastLogin = lastLogin;
    }

    public long getLastLoginTimestamp() {
        return lastLoginTimestamp;
    }

    public void setLastLoginTimestamp(long lastLoginTimestamp) {
        this.lastLoginTimestamp = lastLoginTimestamp;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public long getLastUpdatedTimestamp() {
        return lastUpdatedTimestamp;
    }

    public void setLastUpdatedTimestamp(long lastUpdatedTimestamp) {
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
    }

    public String getLoginProvider() {
        return loginProvider;
    }

    public void setLoginProvider(String loginProvider) {
        this.loginProvider = loginProvider;
    }

    public String getOldestDataUpdated() {
        return oldestDataUpdated;
    }

    public void setOldestDataUpdated(String oldestDataUpdated) {
        this.oldestDataUpdated = oldestDataUpdated;
    }

    public long getOldestDataUpdatedTimestamp() {
        return oldestDataUpdatedTimestamp;
    }

    public void setOldestDataUpdatedTimestamp(long oldestDataUpdatedTimestamp) {
        this.oldestDataUpdatedTimestamp = oldestDataUpdatedTimestamp;
    }

    public Preferences getPreferences() {
        return preferences;
    }

    public void setPreferences(Preferences preferences) {
        this.preferences = preferences;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public String getRegistered() {
        return registered;
    }

    public void setRegistered(String registered) {
        this.registered = registered;
    }

    public long getRegisteredTimestamp() {
        return registeredTimestamp;
    }

    public void setRegisteredTimestamp(long registeredTimestamp) {
        this.registeredTimestamp = registeredTimestamp;
    }

    public SessionInfo getSessionInfo() {
        return sessionInfo;
    }

    public void setSessionInfo(SessionInfo sessionInfo) {
        this.sessionInfo = sessionInfo;
    }

    public long getSignatureTimestamp() {
        return signatureTimestamp;
    }

    public void setSignatureTimestamp(long signatureTimestamp) {
        this.signatureTimestamp = signatureTimestamp;
    }

    public String getSocialProviders() {
        return socialProviders;
    }

    public void setSocialProviders(String socialProviders) {
        this.socialProviders = socialProviders;
    }

    public String getVerified() {
        return verified;
    }

    public void setVerified(String verified) {
        this.verified = verified;
    }

    public long getVerifiedTimestamp() {
        return verifiedTimestamp;
    }

    public void setVerifiedTimestamp(long verifiedTimestamp) {
        this.verifiedTimestamp = verifiedTimestamp;
    }

    public String getRegToken() {
        return regToken;
    }

    public void setRegToken(String regToken) {
        this.regToken = regToken;
    }

    //endregion

}
