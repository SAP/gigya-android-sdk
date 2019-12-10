package com.gigya.android.gigyademo.model;

public class DataEvent {

    private int action;
    private Object data;
    private boolean observed = false;

    public DataEvent(int action, Object data) {
        this.action = action;
        this.data = data;
    }

    public int getAction() {
        return action;
    }

    public Object getData() {
        return data;
    }

    public boolean isObserved() {
        return observed;
    }

    public void setObserved(boolean observed) {
        this.observed = observed;
    }

    public static final int ROUTE_LOGIN_SUCCESS = 0;
    public static final int ROUTE_OPERATION_CANCELED = 1;
    public static final int ROUTE_FORGOT_PASSWORD_EMAIL_SENT = 2;
    public static final int ROUTE_GET_ACCOUNT_INFO = 3;
    public static final int ROUTE_TFA_PROVIDER_SELECTION = 4;
    public static final int ROUTE_TFA_REGISTER_PHONE = 5;
    public static final int ROUTE_TFA_VERIFY_PHONE = 6;
    public static final int ROUTE_TFA_REGISTER_TOTP = 7;
    public static final int ROUTE_TFA_VERIFY_TOTP = 8;
    public static final int ROUTE_AUTH_DEVICE_REGISTER = 9;
    public static final int ROUTE_PENDING_REGISTRATION = 10;
}
