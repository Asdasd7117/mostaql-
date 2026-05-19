package com.example.servicesapp.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Review(
    @SerialName("id")
    val id: Int? = null,
    
    @SerialName("project_id")
    val projectId: Int,
    
    @SerialName("user_id")
    val userId: String,
    
    @SerialName("rating")
    val rating: String,
    
    @SerialName("created_at")
    val createdAt: String? = null
)