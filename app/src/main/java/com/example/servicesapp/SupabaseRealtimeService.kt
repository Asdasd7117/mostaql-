package com.example.servicesapp.chat

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
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

    // دالة مساعدة لإظهار الـ Toast بأمان من داخل العمليات الخلفية (Background Threads)
    private fun showToastOnMainThread(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createServiceNotification()
        startForeground(101, notification)

        // تنبيه بأن الخدمة بدأت الاستماع فعلياً
        showToastOnMainThread("📱 الخدمة الخلفية بدأت الاستماع الآن...")
        startListeningToMessages()

        return START_STICKY
    }

    private fun startListeningToMessages() {
        serviceScope.launch {
            try {
                SupabaseClient.client.realtime.connect()
                
                realtimeChannel = SupabaseClient.client.realtime.channel("messages-database-channel")
                
                val changeFlow = realtimeChannel?.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "messages"
                }
                
                realtimeChannel?.subscribe()
                showToastOnMainThread("✅ متصل بـ سوبابيز ومستعد لاستقبال الأحداث!")

                changeFlow?.collect { action ->
                    if (action is PostgresAction.Insert) {
                        showToastOnMainThread("🔔 التقطت الخدمة رسالة جديدة قادمة للسيرفر!")
                        try {
                            val record = action.record
                            
                            val text = record["message_text"]?.jsonPrimitive?.content ?: ""
                            val senderId = record["sender_id"]?.jsonPrimitive?.content ?: ""
                            val conversationId = record["conversation_id"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                            
                            val currentUserId = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id?.toString()

                            // فحص الشروط وإظهار رسالة توضيحية فورية على الشاشة عند الفشل
                            if (currentUserId.isNullOrBlank()) {
                                showToastOnMainThread("❌ فشل الإشعار: لم يتم العثور على جلسة مستخدم (currentUserId فارغ)!")
                                return@collect
                            }
                            if (senderId == currentUserId) {
                                showToastOnMainThread("🛑 تم تخطي الإشعار: الرسالة مرسلة منك أنت.")
                                return@collect
                            }
                            if (text.isEmpty()) {
                                showToastOnMainThread("❌ تم تخطي الإشعار: نص الرسالة المستلمة فارغ.")
                                return@collect
                            }

                            showToastOnMainThread("🎉 نجحت الشروط! جاري بناء وإظهار الإشعار المرئي لنص: $text")
                            showIncomingMessageNotification(text, conversationId, senderId)

                        } catch (parseEx: Exception) {
                            showToastOnMainThread("❌ خطأ أثناء معالجة بيانات السجل!")
                        }
                    }
                }
            } catch (e: Exception) {
                showToastOnMainThread("❌ انهيار اتصال الـ Realtime بالخلفية: ${e.localizedMessage}")
            }
        }
    }

    private fun showIncomingMessageNotification(messageText: String, convId: Long, senderId: String) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("conversationId", convId)
            putExtra("otherUserId", senderId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 
            convId.toInt(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val chatNotification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("رسالة جديدة")
            .setContentText(messageText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(convId.toInt(), chatNotification)
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
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        showToastOnMainThread("⚠️ تم إغلاق وتدمير الخدمة الخلفية!")
        serviceScope.launch {
            try {
                realtimeChannel?.unsubscribe()
            } catch (e: Exception) {
                // خطأ غير مؤثر عند التدمير
            }
            serviceScope.cancel()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
