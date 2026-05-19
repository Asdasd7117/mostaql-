package com.example.servicesapp.data

import com.example.servicesapp.SupabaseClient
import com.example.servicesapp.models.Service
import com.example.servicesapp.utils.DB
import io.github.jan.supabase.postgrest.from

object ServiceRepository {

    suspend fun addService(service: Service): Service {
        return SupabaseClient.client
            .from(DB.SERVICES)
            .insert(service) {
                select()
            }
            .decodeSingle<Service>()
    }

    suspend fun getServices(): List<Service> {
        return SupabaseClient.client
            .from(DB.SERVICES)
            .select()
            .decodeList<Service>()
    }

    suspend fun getServiceById(id: Int): Service? {
        return SupabaseClient.client
            .from(DB.SERVICES)
            .select {
                filter { eq("id", id) }
            }
            .decodeSingleOrNull<Service>()
    }

    // ✅ تأكد أن id ليس Int?
    suspend fun deleteService(id: Int) {
        SupabaseClient.client
            .from(DB.SERVICES)
            .delete {
                filter { eq("id", id) }
            }
    }

    suspend fun updateService(service: Service) {
        SupabaseClient.client
            .from(DB.SERVICES)
            .update(service) {
                filter { eq("id", service.id ?: return) }
            }
    }
}