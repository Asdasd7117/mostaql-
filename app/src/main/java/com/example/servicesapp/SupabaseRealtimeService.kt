package com.example.servicesapp.chat

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.servicesapp.R
import com.example.servicesapp.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive

class SupabaseRealtimeService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val channelId = "foreground_chat_channel"
    private var realtimeChannel: RealtimeChannel? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createServiceNotification()
        startForeground(101, notification)

        startListeningToMessages()

        return START_STICKY
    }

    private fun startListeningToMessages() {
        serviceScope.launch {
            try {
                SupabaseClient.client.realtime.connect()
                
                realtimeChannel = SupabaseClient.client.realtime.channel("global-notification-room")
                
                val changeFlow = realtimeChannel?.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "messages"
                }
                
                realtimeChannel?.subscribe()

                changeFlow?.collect { action ->
                    if (action is PostgresAction.Insert) {
                        try {
                            val record = action.record
                            val text = record["message_text"]?.jsonPrimitive?.content ?: ""
                            val senderId = record["sender_id"]?.jsonPrimitive?.content ?: ""
                            val currentUserId = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id?.toString()

                            if (!currentUserId.isNullOrBlank() && senderId != currentUserId && text.isNotEmpty()) {
                                showIncomingMessageNotification(text)
                            }
                        } catch (parseEx: Exception) {
                            Log.e("RealtimeService", "Error parsing background record", parseEx)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("RealtimeService", "Service Realtime connect error", e)
            }
        }
    }

    private fun showIncomingMessageNotification(messageText: String) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val chatNotification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("رسالة جديدة")
            .setContentText(messageText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), chatNotification)
    }

    private fun createServiceNotification(): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle("خدمة المراسلة الفورية")
            .setContentText("التطبيق مستعد لاستقبال الرسائل في الخلفية")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Chat Background Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        serviceScope.launch {
            try {
                realtimeChannel?.unsubscribe()
            } catch (e: Exception) {
                Log.e("RealtimeService", "Unsubscribe error on destroy", e)
            }
            serviceScope.cancel()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
