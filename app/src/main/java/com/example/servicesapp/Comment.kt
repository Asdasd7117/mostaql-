package com.example.servicesapp.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    @SerialName("id")
    val id: Int? = null,
    
    @SerialName("project_id")
    val projectId: Int,
    
    @SerialName("user_id")
    val userId: String,
    
    @SerialName("comment_text")
    val commentText: String,
    
    @SerialName("created_at")
    val createdAt: String? = null
)