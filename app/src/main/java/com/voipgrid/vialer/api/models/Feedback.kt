package com.voipgrid.vialer.api.models

import android.os.Build
import com.google.gson.annotations.SerializedName
import com.voipgrid.vialer.User
import com.voipgrid.vialer.VialerApplication

data class Feedback(val message: String, val user: FeedbackUser = FeedbackUser(), val application: FeedbackApplication = FeedbackApplication()) {

    data class FeedbackUser(
            val id: String = User.uuid,
            @SerializedName("email_address") val emailAddress: String = User.username,
            @SerializedName("given_name") val givenName: String = User.voipgridUser?.firstName ?: "",
            @SerializedName("family_name") val familyName: String = User.voipgridUser?.lastName ?: ""
    )


    data class FeedbackApplication(
            val id: String = VialerApplication.get().packageName,
            val version: String = VialerApplication.getAppVersion(),
            val os: String = "android",
            @SerializedName("os_version") val osVersion: String = Build.VERSION.RELEASE,
            @SerializedName("device_info") val deviceInfo: String = Build.MODEL
    )
}