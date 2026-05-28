package com.example.servicesapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.servicesapp.chat.ChatListActivity
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SupabaseRealtimeService(private val context: Context) {

    fun startListening() {

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val channel = SupabaseClient.client.channel("messages-channel")

                channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "messages"
                }.collect { action ->
                    Log.d("SupabaseRealtime", "Data received: $action")
                    showNotification("رسالة جديدة")
                }

                channel.subscribe()
                Log.d("SupabaseRealtime", "Subscribed successfully")
                
            } catch (e: Exception) {
                Log.e("SupabaseRealtime", "Error: ${e.message}")
            }
        }
    }

    private fun showNotification(message: String) {

        val channelId = "chat_channel"

        val intent = Intent(context, ChatListActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val notificationChannel = NotificationChannel(
                channelId,
                "Chat Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Chat Notifications Channel"
                enableLights(true)
                enableVibration(true)
            }

            manager.createNotificationChannel(notificationChannel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("رسالة جديدة")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
        Log.d("SupabaseRealtime", "Notification posted")
    }
}
