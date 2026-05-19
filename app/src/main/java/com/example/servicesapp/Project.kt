package com.example.servicesapp.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.example.servicesapp.utils.DB

@Serializable
data class Project(
    @SerialName("id")
    val id: Int? = null,
    
    @SerialName("name")
    val name: String,
    
    @SerialName("description")
    val description: String,
    
    @SerialName("language")
    val language: String,
    
    @SerialName("preview_link")
    val previewLink: String,
    
    @SerialName("github_link")
    val githubLink: String? = null,
    
    @SerialName("user_id")
    val userId: String,
    
    @SerialName("created_at")
    val created_at: String? = null
    // ✅ حُذفت: ownerEmail و ownerCountry - لم نعد نحتاجها
)