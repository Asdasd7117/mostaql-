package com.example.servicesapp.chat

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.servicesapp.R
import com.example.servicesapp.SupabaseClient
import com.example.servicesapp.data.ChatRepository
import com.example.servicesapp.models.Conversation
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ChatListActivity : AppCompatActivity() {

    private lateinit var conversationsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)

        conversationsContainer = findViewById(R.id.conversationsContainer)
        loadConversations()
    }

    private fun loadConversations() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val conversations = ChatRepository.getUserConversations()
                val fetchedUsernames = mutableMapOf<String, String>()
                
                conversations.forEach { conv ->
                    val uid = conv.otherUserId
                    if (!uid.isNullOrBlank() && conv.otherUsername.isNullOrEmpty()) {
                        try {
                            val response = SupabaseClient.client
                                .from("user_profiles")
                                .select(Columns.list("username")) {
                                    filter { eq("id", uid) }
                                }
                            
                            val jsonElement = kotlinx.serialization.json.Json.parseToJsonElement(response.data)
                            var nameFromDb: String? = null
                            
                            if (jsonElement is kotlinx.serialization.json.JsonArray && jsonElement.isNotEmpty()) {
                                nameFromDb = jsonElement[0].jsonObject["username"]?.jsonPrimitive?.content
                            } else if (jsonElement is kotlinx.serialization.json.JsonObject) {
                                nameFromDb = jsonElement["username"]?.jsonPrimitive?.content
                            }
                            
                            if (!nameFromDb.isNullOrEmpty()) {
                                fetchedUsernames[uid] = nameFromDb
                            }
                        } catch (e: Exception) {
                            // Suppress individual network errors
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    conversationsContainer.removeAllViews()
                    
                    if (conversations.isEmpty()) {
                        conversationsContainer.addView(TextView(this@ChatListActivity).apply {
                            text = "لا توجد محادثات بعد"
                            textSize = 16f
                            setTextColor(ContextCompat.getColor(this@ChatListActivity, android.R.color.darker_gray))
                            gravity = android.view.Gravity.CENTER
                            setPadding(0, 48, 0, 48)
                        })
                    } else {
                        conversations.forEach { conv ->
                            val dynamicName = fetchedUsernames[conv.otherUserId ?: ""] ?: conv.otherUsername
                            addConversationItem(conv, dynamicName)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    conversationsContainer.removeAllViews()
                    conversationsContainer.addView(TextView(this@ChatListActivity).apply {
                        text = "خطأ في التحميل"
                        textSize = 16f
                        setTextColor(ContextCompat.getColor(this@ChatListActivity, android.R.color.holo_red_dark))
                        gravity = android.view.Gravity.CENTER
                    })
                }
            }
        }
    }

    private fun addConversationItem(conv: Conversation, resolvedUsername: String?) {
        val finalUsername = resolvedUsername ?: "مستخدم"
        
        val itemView = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 16, 20, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 8) }
            setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val intent = Intent(this@ChatListActivity, ChatActivity::class.java).apply {
                    putExtra("conversationId", conv.id ?: -1L)
                    putExtra("otherUserId", conv.otherUserId ?: "")
                    putExtra("otherUsername", finalUsername)
                    putExtra("projectId", conv.projectId)
                    putExtra("projectName", conv.projectName)
                }
                startActivity(intent)
            }
        }

        val avatar = TextView(this).apply {
            text = finalUsername.firstOrNull()?.toString()?.uppercase() ?: "؟"
            textSize = 18f
            setTextColor(ContextCompat.getColor(this@ChatListActivity, android.R.color.white))
            setBackgroundResource(R.drawable.language_badge)
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 16, 0)
            }
        }
        itemView.addView(avatar)

        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val title = TextView(this).apply {
            text = if (!conv.projectName.isNullOrEmpty()) {
                "مشروع: ${conv.projectName}"
            } else {
                finalUsername
            }
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@ChatListActivity, android.R.color.black))
        }
        infoLayout.addView(title)

        val lastMsg = TextView(this).apply {
            text = conv.lastMessage?.take(50) ?: "ابدأ المحادثة..."
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@ChatListActivity, android.R.color.darker_gray))
            maxLines = 1
        }
        infoLayout.addView(lastMsg)

        itemView.addView(infoLayout)

        val time = TextView(this).apply {
            text = conv.updatedAt?.take(10) ?: ""
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@ChatListActivity, android.R.color.darker_gray))
        }
        itemView.addView(time)

        conversationsContainer.addView(itemView)
    }

    override fun onResume() {
        super.onResume()
        loadConversations()
    }
}
