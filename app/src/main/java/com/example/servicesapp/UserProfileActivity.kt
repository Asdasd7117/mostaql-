package com.example.servicesapp.profile

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.LinearLayout
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.servicesapp.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val user_id: String? = null,
    val username: String? = null,
    val username_changed: Boolean = false,
    val email: String? = null
)

class UserProfileActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var btnSave: Button
    private var currentUserEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            gravity = Gravity.CENTER
        }

        etUsername = EditText(this).apply {
            hint = "Username"
            textSize = 16f
            setPadding(20, 20, 20, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 32)
            }
        }

        btnSave = Button(this).apply {
            text = "Save"
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                saveUsername()
            }
        }

        layout.addView(etUsername)
        layout.addView(btnSave)
        setContentView(layout)

        loadUsername()
    }

    private fun loadUsername() {
        lifecycleScope.launch {
            try {
                val session = SupabaseClient.client.auth.currentSessionOrNull()
                val userId = session?.user?.id
                currentUserEmail = session?.user?.email
                
                if (userId != null) {
                    val profiles = SupabaseClient.client
                        .from("user_profiles")
                        .select {
                            filter {
                                eq("user_id", userId)
                            }
                        }
                        .decodeList<UserProfile>()
                    
                    val profile = profiles.firstOrNull()
                    
                    if (profile != null) {
                        etUsername.setText(profile.username)
                        
                        if (profile.username_changed) {
                            disableEditing()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@UserProfileActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUsername() {
        val usernameInput = etUsername.text.toString().trim().lowercase()
        
        if (usernameInput.isBlank() || usernameInput.startsWith("user_")) {
            Toast.makeText(this, "⚠️ يرجى إدخال اسم مستخدم حقيقي ولا يبدأ بـ user_", Toast.LENGTH_LONG).show()
            return
        }

        if (usernameInput.length < 3) {
            Toast.makeText(this, "Minimum 3 characters", Toast.LENGTH_SHORT).show()
            return
        }
        
        btnSave.isEnabled = false
        btnSave.text = "Saving..."
        
        lifecycleScope.launch {
            try {
                val session = SupabaseClient.client.auth.currentSessionOrNull()
                val userId = session?.user?.id
                if (currentUserEmail == null) {
                    currentUserEmail = session?.user?.email
                }
                
                if (userId != null) {
                    val profiles = SupabaseClient.client
                        .from("user_profiles")
                        .select {
                            filter { eq("user_id", userId) }
                        }
                        .decodeList<UserProfile>()
                    
                    val existingProfile = profiles.firstOrNull()
                    
                    if (existingProfile?.username_changed == true) {
                        Toast.makeText(this@UserProfileActivity, "Cannot change again", Toast.LENGTH_SHORT).show()
                        disableEditing()
                        return@launch
                    }
                    
                    val updatedProfile = UserProfile(
                        user_id = userId,
                        username = usernameInput,
                        username_changed = true,
                        email = currentUserEmail ?: existingProfile?.email
                    )
                    
                    SupabaseClient.client
                        .from("user_profiles")
                        .upsert(updatedProfile)
                    
                    Toast.makeText(this@UserProfileActivity, "Success", Toast.LENGTH_LONG).show()
                    finish()
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: ""
                if (errorMsg.contains("duplicate key") || errorMsg.contains("unique constraint")) {
                    Toast.makeText(this@UserProfileActivity, "❌ اسم المستخدم هذا مأخوذ مسبقاً! يرجى اختيار اسم آخر.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@UserProfileActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                btnSave.isEnabled = true
                btnSave.text = "Save"
            }
        }
    }

    private fun disableEditing() {
        etUsername.isEnabled = false
        btnSave.isEnabled = false
        btnSave.text = "Locked"
        btnSave.alpha = 0.5f
    }
}
