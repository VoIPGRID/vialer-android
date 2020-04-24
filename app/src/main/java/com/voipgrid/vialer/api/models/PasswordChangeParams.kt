package com.voipgrid.vialer.api.models

import com.google.gson.annotations.SerializedName

data class PasswordChangeParams (

        @SerializedName("email_address")
        val usersEmail: String,
        @SerializedName("current_password")
        val usersCurrentPassword: String,
        @SerializedName("new_password")
        val usersNewPassword: String
)