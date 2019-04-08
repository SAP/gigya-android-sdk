package com.gigya.android.sample.model

/**
 * Helper data object for country code list.
 * Reference data list located in the assets folder { countryCodes.json }
 */
data class CountryCode(val name: String, val dial_code: String, val code: String) {

    override fun toString(): String {
        return this.name
    }
}