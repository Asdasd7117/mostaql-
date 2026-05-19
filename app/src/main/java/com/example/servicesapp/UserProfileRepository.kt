package com.example.servicesapp.data

import com.example.servicesapp.SupabaseClient
import io.github.jan.supabase.gotrue.auth  // ✅ هذا الـ import ضروري
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object UserProfileRepository {

    suspend fun saveUsername(userId: String, username: String): Boolean {
        return try {
            SupabaseClient.client
                .from("user_profiles")
                .insert(
                    buildJsonObject {
                        put("user_id", userId)
                        put("username", username.lowercase().trim())
                    }
                )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getCurrentUsername(): String? {
        return try {
            val session = SupabaseClient.client.auth.currentSessionOrNull()
            val userId = session?.user?.id?.toString() ?: return null

            val result = SupabaseClient.client
                .from("user_profiles")
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<Map<String, Any>>()

            result.firstOrNull()?.get("username") as? String
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}