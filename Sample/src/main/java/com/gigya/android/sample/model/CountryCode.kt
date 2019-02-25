package com.gigya.android.sample.model

data class CountryCode(val name: String, val dial_code: String, val code: String) {

    override fun toString(): String {
        return this.name
    }
}