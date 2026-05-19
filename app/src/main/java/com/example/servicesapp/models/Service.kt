package com.example.servicesapp.models

import kotlinx.serialization.Serializable

@Serializable
data class Service(
    val id: Int? = null,
    val title: String,
    val description: String,
    val price: Double,
    val user_id: String
)