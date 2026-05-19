package com.example.servicesapp.data

import com.example.servicesapp.SupabaseClient
import com.example.servicesapp.models.Project
import com.example.servicesapp.utils.DB
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.gotrue.auth

object ProjectRepository {

    suspend fun addProject(project: Project) {
        SupabaseClient.client
            .from(DB.PROJECTS)
            .insert(project)
    }

    suspend fun getAllProjects(): List<Project> {
        return try {
            SupabaseClient.client
                .from(DB.PROJECTS)
                .select()
                .decodeList<Project>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getUserProjects(userId: String): List<Project> {
        return try {
            SupabaseClient.client
                .from(DB.PROJECTS)
                .select {
                    filter { eq(DB.PROJECT_USER, userId) }
                }
                .decodeList<Project>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun deleteProject(projectId: Int, userId: String): Boolean {
        return try {
            SupabaseClient.client
                .from(DB.PROJECTS)
                .delete {
                    filter {
                        eq("id", projectId)
                        eq(DB.PROJECT_USER, userId)
                    }
                }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
