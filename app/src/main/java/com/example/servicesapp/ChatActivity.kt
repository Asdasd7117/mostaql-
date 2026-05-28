package com.example.servicesapp.chat

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.servicesapp.R
import com.example.servicesapp.SupabaseClient
import com.example.servicesapp.data.ChatRepository
import com.example.servicesapp.models.Conversation
import com.example.servicesapp.models.Message
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.decodeRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.datetime.Clock

class ChatActivity : AppCompatActivity() {

    private lateinit var messagesContainer: LinearLayout
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var tvTitle: TextView
    private lateinit var scrollView: ScrollView

    private var conversationId: Long? = null
    private var otherUserId: String? = null
    private var currentUserId: String? = null
    private var otherUsername: String? = null
    private var realtimeChannel: RealtimeChannel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        initViews()
        loadData()
        setupListeners()
    }

    private fun initViews() {
        tvTitle = findViewById(R.id.tvTitle)
        messagesContainer = findViewById(R.id.messagesContainer)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        scrollView = findViewById(R.id.scrollView)
    }

    private fun loadData() {
        conversationId = intent.getLongExtra("conversationId", -1L).takeIf { it != -1L }
        otherUserId = intent.getStringExtra("otherUserId")
        val projectName = intent.getStringExtra("projectName")
        
        currentUserId = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id?.toString()
        
        if (conversationId == null) {
            Toast.makeText(this, "Error: Invalid ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        if (currentUserId.isNullOrBlank()) {
            Toast.makeText(this, "Error: No Session", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (otherUserId.isNullOrBlank()) {
            fetchConversationDetails(projectName)
        } else {
            fetchOtherUsername(projectName)
        }
        
        loadMessages()
        observeRealtimeMessages()
    }

    private fun fetchConversationDetails(projectName: String?) {
        val id = conversationId ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = SupabaseClient.client
                    .from("conversations")
                    .select { filter { eq("id", id) } }
                    .decodeSingle<Conversation>()
                
                otherUserId = if (currentUserId == response.user1Id) response.user2Id else response.user1Id
                
                withContext(Dispatchers.Main) {
                    fetchOtherUsername(projectName)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvTitle.text = "Err: Conv Detail Fail"
                }
            }
        }
    }

    private fun fetchOtherUsername(projectName: String?) {
        val uid = otherUserId ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = SupabaseClient.client
                    .from("user_profiles")
                    .select {
                        filter { eq("user_id", uid) }
                    }

                val jsonElement = Json.parseToJsonElement(response.data)
                
                var nameFromDb: String? = null
                if (jsonElement is kotlinx.serialization.json.JsonArray && jsonElement.isNotEmpty()) {
                    nameFromDb = jsonElement[0].jsonObject["username"]?.jsonPrimitive?.content
                } else if (jsonElement is kotlinx.serialization.json.JsonObject) {
                    nameFromDb = jsonElement["username"]?.jsonPrimitive?.content
                }
                
                withContext(Dispatchers.Main) {
                    if (!nameFromDb.isNullOrEmpty()) {
                        otherUsername = nameFromDb
                        updateTitle(projectName)
                    } else {
                        tvTitle.text = "Chat with: User"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvTitle.text = "Err: ${e.message?.take(15)}"
                    Log.e("ChatActivity", "Fetch error", e)
                }
            }
        }
    }

    private fun updateTitle(projectName: String?) {
        tvTitle.text = if (!projectName.isNullOrEmpty()) {
            "Project: $projectName"
        } else {
            "Chat with ${otherUsername ?: "User"}"
        }
    }

    private fun loadMessages() {
        val convId = conversationId ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val messages = ChatRepository.getConversationMessages(convId)
                withContext(Dispatchers.Main) {
                    messagesContainer.removeAllViews()
                    if (messages.isEmpty()) {
                        messagesContainer.addView(TextView(this@ChatActivity).apply {
                            text = "No messages yet..."
                            textSize = 14f
                            gravity = Gravity.CENTER
                            setPadding(0, 40, 0, 40)
                        })
                    } else {
                        messages.forEach { addMessageBubble(it) }
                    }
                    scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
                }
                currentUserId?.let { ChatRepository.markMessagesAsRead(convId, it) }
            } catch (e: Exception) {
                Log.e("ChatActivity", "Load messages error", e)
            }
        }
    }

    private fun observeRealtimeMessages() {
        val convId = conversationId ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                realtimeChannel = SupabaseClient.client.channel("chat-room-$convId")
                
                realtimeChannel?.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "messages"
                }?.collect { action ->
                    if (action is PostgresAction.Insert) {
                        try {
                            val message = action.decodeRecord<Message>()
                            if (message.conversationId == convId) {
                                withContext(Dispatchers.Main) {
                                    if (messagesContainer.childCount == 1 && messagesContainer.getChildAt(0) is TextView) {
                                        val firstChild = messagesContainer.getChildAt(0) as TextView
                                        if (firstChild.text == "No messages yet...") {
                                            messagesContainer.removeAllViews()
                                        }
                                    }
                                    addMessageBubble(message)
                                    scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
                                }
                                currentUserId?.let { uid ->
                                    if (message.senderId != uid) {
                                        ChatRepository.markMessagesAsRead(convId, uid)
                                    }
                                }
                            }
                        } catch (parseEx: Exception) {
                            Log.e("ChatActivity", "Realtime parse error", parseEx)
                        }
                    }
                }
                
                realtimeChannel?.subscribe()
            } catch (e: Exception) {
                Log.e("ChatActivity", "Realtime connect error", e)
            }
        }
    }

    private fun addMessageBubble(msg: Message) {
        val isMyMessage = msg.senderId == currentUserId
        val bubbleContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 4, 0, 4) }
            gravity = if (isMyMessage) Gravity.END else Gravity.START
        }
        
        val bubble = TextView(this).apply {
            text = msg.messageText
            textSize = 15f
            setPadding(16, 12, 16, 12)
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply {
                marginStart = if (isMyMessage) 0 else 8
                marginEnd = if (isMyMessage) 8 else 0
                maxWidth = (resources.displayMetrics.widthPixels * 0.7).toInt()
            }
            if (isMyMessage) {
                setBackgroundColor(ContextCompat.getColor(this@ChatActivity, android.R.color.holo_blue_light))
                setTextColor(android.graphics.Color.WHITE)
            } else {
                setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
                setTextColor(android.graphics.Color.BLACK)
            }
        }
        bubbleContainer.addView(bubble)
        messagesContainer.addView(bubbleContainer)
    }

    private fun setupListeners() {
        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty() && conversationId != null && !otherUserId.isNullOrBlank()) {
                sendMessage(text)
            }
        }
    }

    private fun sendMessage(text: String) {
        val convId = conversationId ?: return
        val sender = currentUserId ?: return
        val receiver = otherUserId ?: return
        
        btnSend.isEnabled = false
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val success = ChatRepository.sendMessage(convId, sender, receiver, text)
                withContext(Dispatchers.Main) {
                    btnSend.isEnabled = true
                    if (success) {
                        etMessage.setText("")
                        loadMessages()
                        updateLastMessageInConversation(convId, text)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnSend.isEnabled = true
                    Toast.makeText(this@ChatActivity, "Send Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateLastMessageInConversation(convId: Long, message: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                SupabaseClient.client.from("conversations").update(buildJsonObject {
                    put("last_message", message)
                    put("updated_at", Clock.System.now().toString())
                }) { filter { eq("id", convId) } }
            } catch (e: Exception) { 
                Log.e("ChatActivity", "Update failed", e) 
            }
        }
    }

    override fun onDestroy() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                realtimeChannel?.unsubscribe()
            } catch (e: Exception) {
                Log.e("ChatActivity", "Unsubscribe error", e)
            }
        }
        super.onDestroy()
    }
}
