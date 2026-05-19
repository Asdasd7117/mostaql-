package com.example.servicesapp.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    @SerialName("id")
    val id: Long? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("username")
    val username: String,
    @SerialName("email")
    val email: String? = null,
    @SerialName("device_id")
    val deviceId: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)