package com.gigya.android.sample.ui.common

/**
 * Centralised test tag constants for all interactive UI elements.
 *
 * Every composable that a test needs to locate or assert against must use one
 * of these constants as its [androidx.compose.ui.platform.testTag]. This
 * single file is the authoritative reference for what is addressable in tests.
 */
object TestTags {

    // region Login screen
    const val INPUT_EMAIL = "input_email"
    const val INPUT_PASSWORD = "input_password"
    const val BTN_LOGIN = "btn_login"
    const val BTN_REGISTER = "btn_register"
    const val BTN_PASSKEY_LOGIN = "btn_passkey_login"
    const val BTN_OTP_LOGIN = "btn_otp_login"
    const val DROPDOWN_SOCIAL_PROVIDER = "dropdown_social_provider"
    const val BTN_SOCIAL_LOGIN = "btn_social_login"
    const val BTN_SSO = "btn_sso"
    // endregion

    // region Account screen
    const val TEXT_UID = "text_uid"
    const val BTN_GET_ACCOUNT = "btn_get_account"
    const val BTN_LOGOUT = "btn_logout"
    const val DROPDOWN_CONNECTION_PROVIDER = "dropdown_connection_provider"
    const val BTN_ADD_CONNECTION = "btn_add_connection"
    const val BTN_REMOVE_CONNECTION = "btn_remove_connection"
    const val BTN_PASSKEY_REGISTER = "btn_passkey_register"
    const val BTN_PASSKEY_REVOKE = "btn_passkey_revoke"
    const val BTN_PASSKEY_GET = "btn_passkey_get"
    const val TEXT_PASSKEY_RESULT = "text_passkey_result"
    const val TEXT_BIOMETRIC_STATUS = "text_biometric_status"
    const val BTN_BIOMETRIC_OPT = "btn_biometric_opt"
    const val BTN_BIOMETRIC_LOCK = "btn_biometric_lock"
    const val BTN_PUSH_TFA_OPT_IN = "btn_push_tfa_opt_in"
    const val BTN_PUSH_AUTH_OPT_IN = "btn_push_auth_opt_in"
    // endregion

    // region TFA screen
    const val DROPDOWN_TFA_PROVIDER = "dropdown_tfa_provider"
    const val IMAGE_QR_CODE = "image_qr_code"
    const val INPUT_PHONE_NUMBER = "input_phone_number"
    const val BTN_REGISTER_PHONE = "btn_register_phone"
    const val TEXT_EMAIL_TFA_HINT = "text_email_tfa_hint"
    const val INPUT_TFA_CODE = "input_tfa_code"
    const val BTN_TFA_VERIFY = "btn_tfa_verify"
    // endregion

    // region Link account screen
    const val DROPDOWN_LINK_PROVIDER = "dropdown_link_provider"
    const val INPUT_LINK_PASSWORD = "input_link_password"
    const val BTN_LINK = "btn_link"
    // endregion

    // region OTP screen
    const val INPUT_OTP_PHONE = "input_otp_phone"
    const val BTN_OTP_SEND = "btn_otp_send"
    const val INPUT_OTP_CODE = "input_otp_code"
    const val BTN_OTP_VERIFY = "btn_otp_verify"
    // endregion

    // region Settings screen
    const val INPUT_API_KEY = "input_api_key"
    const val INPUT_DATA_CENTER = "input_data_center"
    const val INPUT_CNAME = "input_cname"
    const val BTN_REINIT = "btn_reinit"
    // endregion

    // region Shared
    const val TEXT_STATUS = "text_status"
    // endregion
}
