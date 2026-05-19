package com.example.servicesapp.data

import com.example.servicesapp.SupabaseClient
import com.example.servicesapp.models.Review
import com.example.servicesapp.models.Comment
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import io.github.jan.supabase.gotrue.auth

object ReviewRepository {

    suspend fun submitReview(projectId: Int, rating: String): Boolean {
        return try {
            val session = SupabaseClient.client.auth.currentSessionOrNull()
            val userId = session?.user?.id?.toString() ?: return false

            SupabaseClient.client.from("reviews").delete {
                filter { eq("project_id", projectId); eq("user_id", userId) }
            }

            SupabaseClient.client.from("reviews").insert(buildJsonObject {
                put("project_id", projectId)
                put("user_id", userId)
                put("rating", rating)
            })
            true
        } catch (e: Exception) { false }
    }

    suspend fun addComment(projectId: Int, commentText: String): Boolean {
        return try {
            val session = SupabaseClient.client.auth.currentSessionOrNull()
            val userId = session?.user?.id?.toString() ?: return false

            SupabaseClient.client.from("comments").insert(buildJsonObject {
                put("project_id", projectId)
                put("user_id", userId)
                put("comment_text", commentText)
            })
            true
        } catch (e: Exception) { false }
    }

    suspend fun getProjectReviews(projectId: Int): List<Review> {
        return try {
            SupabaseClient.client.from("reviews")
                .select { filter { eq("project_id", projectId) } }
                .decodeList<Review>()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getProjectComments(projectId: Int): List<Comment> {
        return try {
            SupabaseClient.client.from("comments")
                .select { filter { eq("project_id", projectId) } }
                .decodeList<Comment>()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getRatingCounts(projectId: Int): Map<String, Int> {
        return try {
            val reviews = getProjectReviews(projectId)
            mapOf(
                "weak" to reviews.count { it.rating == "weak" },
                "good" to reviews.count { it.rating == "good" },
                "excellent" to reviews.count { it.rating == "excellent" }
            )
        } catch (e: Exception) { mapOf("weak" to 0, "good" to 0, "excellent" to 0) }
    }
}