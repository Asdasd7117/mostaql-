package com.example.servicesapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.servicesapp.chat.ChatListActivity
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SupabaseRealtimeService(private val context: Context) {

    fun startListening() {

        CoroutineScope(Dispatchers.IO).launch {

            val channel = SupabaseClient.client.realtime.channel("public:messages")

            channel.postgresChangeFlow(
                schema = "public",
                table = "messages",
                event = PostgresAction.INSERT
            ).collect { change ->

                val text = change.record["text"]?.toString() ?: "رسالة جديدة"

                showNotification(text)
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                channelId,
                "Chat Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("رسالة جديدة")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context)
            .notify(System.currentTimeMillis().toInt(), notification)
    }
}
