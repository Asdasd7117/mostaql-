package com.example.servicesapp.admin

import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.servicesapp.R
import com.example.servicesapp.SupabaseClient
import com.example.servicesapp.data.AdminRepository
import com.example.servicesapp.models.Project
import com.example.servicesapp.profile.UserProfile
import com.example.servicesapp.utils.DeviceUtils
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var mainContainer: LinearLayout
    private lateinit var searchEditText: EditText
    private var allUsers = listOf<UserProfile>()
    private var allProjects = listOf<Project>()
    private var allComments = listOf<Map<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)
        
        mainContainer = findViewById(R.id.mainContainer)
        searchEditText = findViewById(R.id.searchEditText)
        
        setupSearch()
        loadAllData()
    }

    private fun loadAllData() {
        lifecycleScope.launch {
            try {
                allUsers = AdminRepository.getAllUsers()
                allProjects = AdminRepository.getAllProjects()
                allComments = AdminRepository.getAllComments()
                
                withContext(Dispatchers.Main) {
                    displayData(allUsers)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AdminDashboardActivity, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun displayData(users: List<UserProfile>) {
        mainContainer.removeAllViews()
        
        if (users.isEmpty()) {
            mainContainer.addView(TextView(this).apply { text = "لا يوجد مستخدمين"; gravity = android.view.Gravity.CENTER })
            return
        }

        users.forEach { user ->
            val userCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(30, 30, 30, 30)
                val params = LinearLayout.LayoutParams(-1, -2)
                params.setMargins(0, 0, 0, 40)
                layoutParams = params
                setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
            }

            val targetUserId = user.user_id ?: ""
            val targetEmail = user.email ?: ""
            val displayEmail = if (targetEmail.isNotBlank()) targetEmail else "لا يوجد إيميل مسجل"
            
            userCard.addView(TextView(this).apply {
                text = "المستخدم: ${user.username ?: "بدون اسم"}"
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setTextColor(android.graphics.Color.BLUE)
            })
            
            userCard.addView(TextView(this).apply { 
                text = "الإيميل: $displayEmail"
                textSize = 14f
                setPadding(0, 5, 0, 10)
            })

            val uProjects = allProjects.filter { 
                val pUserId = it.userId ?: it.user_id ?: ""
                pUserId.trim().lowercase() == targetUserId.trim().lowercase() && targetUserId.isNotBlank()
            }
            
            if (uProjects.isNotEmpty()) {
                userCard.addView(TextView(this).apply { text = "المشاريع:"; setTypeface(null, Typeface.BOLD) })
                uProjects.forEach { p ->
                    val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                    row.addView(TextView(this).apply { text = "• ${p.name}"; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
                    row.addView(Button(this).apply { 
                        text = "حذف"
                        textSize = 10f
                        setOnClickListener { deleteProject(p.id ?: 0) } 
                    })
                    userCard.addView(row)
                }
            }

            val uComments = allComments.filter { 
                val cUserId = it["user_id"]?.toString() ?: it["userId"]?.toString() ?: ""
                cUserId.trim().lowercase() == targetUserId.trim().lowercase() && targetUserId.isNotBlank()
            }
            
            if (uComments.isNotEmpty()) {
                userCard.addView(TextView(this).apply { text = "التعليقات:"; setTypeface(null, Typeface.BOLD); setPadding(0, 10, 0, 0) })
                uComments.forEach { c ->
                    val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                    row.addView(TextView(this).apply { text = "💬 ${c["comment_text"] ?: c["commentText"] ?: ""}"; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
                    row.addView(Button(this).apply { 
                        text = "حذف"
                        textSize = 10f
                        setOnClickListener { deleteComment(c["id"].toString().toDouble().toInt()) } 
                    })
                    userCard.addView(row)
                }
            }

            userCard.addView(Button(this).apply {
                text = "حظر الإيميل نهائياً"
                setBackgroundColor(android.graphics.Color.RED)
                setTextColor(android.graphics.Color.WHITE)
                setOnClickListener { 
                    if (targetEmail.isBlank()) {
                        Toast.makeText(this@AdminDashboardActivity, "الإيميل مفقود للبروفايل الحالي", Toast.LENGTH_SHORT).show()
                    } else {
                        banUserByEmail(targetEmail, targetUserId)
                    }
                }
            })
            
            mainContainer.addView(userCard)
        }
    }

    private fun banUserByEmail(email: String, userId: String) {
        if (userId.isBlank()) return
        lifecycleScope.launch {
            try {
                SupabaseClient.client.from("user_profiles").delete {
                    filter { eq("user_id", userId) }
                }

                SupabaseClient.client.from("projects").delete {
                    filter { eq("user_id", userId) }
                }
                
                SupabaseClient.client.from("comments").delete {
                    filter { eq("user_id", userId) }
                }

                SupabaseClient.client.from("banned_users").insert(mapOf(
                    "email" to email,
                    "user_id" to userId,
                    "reason" to "حظر نهائي ومسح شامل لجميع البيانات"
                ))

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AdminDashboardActivity, "تم حظر $email ومسح كافة بياناته نهائياً", Toast.LENGTH_SHORT).show()
                    loadAllData() 
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AdminDashboardActivity, "خطأ: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun deleteProject(id: Int) {
        lifecycleScope.launch { if (AdminRepository.deleteProject(id)) loadAllData() }
    }

    private fun deleteComment(id: Int) {
        lifecycleScope.launch { if (AdminRepository.deleteComment(id)) loadAllData() }
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val q = s.toString().lowercase().trim()
                val filtered = allUsers.filter { 
                    val uName = (it.username ?: "").lowercase()
                    val uEmail = (it.email ?: "").lowercase()
                    uName.contains(q) || uEmail.contains(q)
                }
                displayData(filtered)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
}
