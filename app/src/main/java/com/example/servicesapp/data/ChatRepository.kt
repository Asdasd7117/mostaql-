package com.example.servicesapp.data

import com.example.servicesapp.SupabaseClient
import com.example.servicesapp.models.Conversation
import com.example.servicesapp.models.Message
import com.example.servicesapp.profile.UserProfile
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object ChatRepository {

    suspend fun getOrCreateConversation(userId1: String, userId2: String, projectId: Long?): Long? {
        return try {
            val existing = SupabaseClient.client
                .from("conversations")
                .select()
                .decodeList<Conversation>()
                .firstOrNull { 
                    (it.user1Id == userId1 && it.user2Id == userId2) || 
                    (it.user1Id == userId2 && it.user2Id == userId1) 
                }
            
            if (existing != null) return existing.id

            val response = SupabaseClient.client
                .from("conversations")
                .insert(
                    buildJsonObject {
                        put("user1_id", userId1)
                        put("user2_id", userId2)
                        if (projectId != null) put("project_id", projectId)
                    }
                ) {
                    select()
                }

            val newConv = response.decodeSingle<Conversation>()
            newConv.id
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun sendMessage(conversationId: Long, senderId: String, receiverId: String, messageText: String): Boolean {
        return try {
            SupabaseClient.client
                .from("messages")
                .insert(
                    buildJsonObject {
                        put("conversation_id", conversationId)
                        put("sender_id", senderId)
                        put("receiver_id", receiverId)
                        put("message_text", messageText)
                    }
                )
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getUserConversations(): List<Conversation> {
        return try {
            val userId = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id?.toString() ?: return emptyList()
            val response = SupabaseClient.client.from("conversations").select().decodeList<Conversation>()
            
            val filteredConversations = response.filter { it.user1Id == userId || it.user2Id == userId }
                .sortedByDescending { it.updatedAt }

            val modifiedList = mutableListOf<Conversation>()

            filteredConversations.forEach { conv ->
                val targetUid = if (conv.user1Id == userId) conv.user2Id else conv.user1Id
                var updatedName: String? = null

                if (!targetUid.isNullOrBlank()) {
                    try {
                        // هنا استخدمنا نفس الطريقة من ملف الـ Profile الخاص بك بالضبط المضمونة 100%
                        val profiles = SupabaseClient.client
                            .from("user_profiles")
                            .select {
                                filter {
                                    eq("user_id", targetUid)
                                }
                            }
                            .decodeList<UserProfile>()
                        
                        val profile = profiles.firstOrNull()
                        if (profile != null) {
                            updatedName = profile.username
                        }
                    } catch (inner: Exception) {
                        // تخطي الأخطاء الفردية واستمرار المعالجة
                    }
                }

                // نمرر الاسم الجديد بدلاً من القيمة الافتراضية الثابتة
                val finalConv = if (!updatedName.isNullOrEmpty()) {
                    conv.copy(otherUsername = updatedName)
                } else {
                    conv
                }
                modifiedList.add(finalConv)
            }
            
            modifiedList
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getConversationMessages(conversationId: Long): List<Message> {
        return try {
            SupabaseClient.client
                .from("messages")
                .select {
                    filter { eq("conversation_id", conversationId) }
                }
                .decodeList<Message>()
                .sortedBy { it.createdAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun markMessagesAsRead(conversationId: Long, userId: String): Boolean {
        return try {
            SupabaseClient.client
                .from("messages")
                .update(buildJsonObject { put("is_read", true) }) {
                    filter { 
                        eq("conversation_id", conversationId)
                        eq("receiver_id", userId) 
                    }
                }
            true
        } catch (e: Exception) {
            false
        }
    }
}
