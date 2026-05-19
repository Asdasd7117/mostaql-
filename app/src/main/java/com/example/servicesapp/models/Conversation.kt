package com.example.servicesapp.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Conversation(
    @SerialName("id")
    val id: Long, // أزلت الاختيارية هنا لأن المعرف ضروري دائماً
    
    @SerialName("user1_id")
    val user1Id: String,
    
    @SerialName("user2_id")
    val user2Id: String,
    
    @SerialName("project_id")
    val projectId: Long? = null,
    
    @SerialName("project_name")
    val projectName: String? = null,
    
    // جعلنا هذه الحقول اختيارية لأنها غالباً تُحسب برمجياً ولا توجد في الجدول
    @SerialName("other_user_id")
    val otherUserId: String? = null,
    
    @SerialName("other_username")
    val otherUsername: String? = null,
    
    @SerialName("last_message")
    val lastMessage: String? = null,
    
    @SerialName("updated_at")
    val updatedAt: String? = null
)
