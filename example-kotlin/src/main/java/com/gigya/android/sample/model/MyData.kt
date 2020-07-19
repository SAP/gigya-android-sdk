package com.gigya.android.sample.model


data class MyData(var subscribe: Boolean?,
                  var terms: Boolean?,
                  var comment: String?,
                  var favGuitar: String?,
                  var rescueString: String?,
                  var marketingNotifications: Boolean? = false,
                  var formatTrue: Boolean? = false)