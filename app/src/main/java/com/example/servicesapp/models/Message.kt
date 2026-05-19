package com.example.servicesapp.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    @SerialName("id")
    val id: Long? = null,
    
    @SerialName("conversation_id")
    val conversationId: Long,
    
    @SerialName("sender_id")
    val senderId: String,
    
    @SerialName("receiver_id")
    val receiverId: String,
    
    @SerialName("message_text")
    val messageText: String,
    
    @SerialName("is_read")
    val isRead: Boolean = false,
    
    @SerialName("created_at")
    val createdAt: String? = null
)