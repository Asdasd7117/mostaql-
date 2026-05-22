package com.example.servicesapp.data

import com.example.servicesapp.SupabaseClient
import com.example.servicesapp.models.Project
import com.example.servicesapp.profile.UserProfile
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object AdminRepository {

    suspend fun getAllUsers(): List<UserProfile> {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client
                    .from("user_profiles")
                    .select()
                    .decodeList<UserProfile>()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun getAllProjects(): List<Project> {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client
                    .from("projects")
                    .select()
                    .decodeList<Project>()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun deleteProject(projectId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client
                    .from("projects")
                    .delete {
                        filter { eq("id", projectId) }
                    }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun banUser(userId: String, deviceId: String, reason: String, adminId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client
                    .from("banned_users")
                    .insert(
                        buildJsonObject {
                            put("user_id", userId)
                            put("device_id", deviceId)
                            put("reason", reason)
                            put("banned_by", adminId)
                        }
                    )
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun unbanUser(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client
                    .from("banned_users")
                    .delete {
                        filter { eq("user_id", userId) }
                    }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun isUserBanned(userId: String, deviceId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val bannedByUser = SupabaseClient.client
                    .from("banned_users")
                    .select {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<Map<String, Any>>()
                
                if (bannedByUser.isNotEmpty()) return@withContext true
                
                val bannedByDevice = SupabaseClient.client
                    .from("banned_users")
                    .select {
                        filter { eq("device_id", deviceId) }
                    }
                    .decodeList<Map<String, Any>>()
                
                bannedByDevice.isNotEmpty()
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun updateUserProfileDeviceId(userId: String, deviceId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client
                    .from("user_profiles")
                    .update(
                        buildJsonObject {
                            put("device_id", deviceId)
                        }
                    ) {
                        filter { eq("user_id", userId) }
                    }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun getAllComments(): List<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client
                    .from("comments")
                    .select()
                    .decodeList<Map<String, Any>>()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun deleteComment(commentId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client
                    .from("comments")
                    .delete {
                        filter { eq("id", commentId) }
                    }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
