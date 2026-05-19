package com.example.servicesapp.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.servicesapp.MainActivity
import com.example.servicesapp.R
import com.example.servicesapp.SupabaseClient
import com.example.servicesapp.admin.AdminDashboardActivity
import com.example.servicesapp.data.AdminRepository
import com.example.servicesapp.utils.DeviceUtils
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var loginBtn: Button
    private lateinit var registerBtn: Button
    private lateinit var titleText: TextView
    private lateinit var loginBanner: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        loginBanner = findViewById(R.id.loginBanner)
        titleText = findViewById(R.id.titleText)
        email = findViewById(R.id.email)
        password = findViewById(R.id.password)
        loginBtn = findViewById(R.id.loginBtn)
        registerBtn = findViewById(R.id.registerBtn)
    }

    private fun setupClickListeners() {
        loginBtn.setOnClickListener {
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()

            if (emailText.isEmpty() || passwordText.isEmpty()) {
                Toast.makeText(this, "الرجاء إدخال البريد الإلكتروني وكلمة المرور", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUser(emailText, passwordText)
        }

        registerBtn.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }

    private fun loginUser(email: String, password: String) {
        loginBtn.isEnabled = false
        loginBtn.text = "جاري تسجيل الدخول..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                SupabaseClient.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }

                val user = SupabaseClient.client.auth.currentUserOrNull()
                
                if (user != null) {
                    val userId = user.id.toString()
                    val userEmail = user.email.toString().lowercase().trim()
                    val deviceId = DeviceUtils.getDeviceId(this@LoginActivity)
                    
                    AdminRepository.updateUserProfileDeviceId(userId, deviceId)
                    
                    val isBanned = AdminRepository.isUserBanned(userId, deviceId)
                    
                    if (isBanned) {
                        withContext(Dispatchers.Main) {
                            SupabaseClient.client.auth.signOut()
                            Toast.makeText(this@LoginActivity, "حسابك محظور من استخدام التطبيق", Toast.LENGTH_LONG).show()
                            loginBtn.isEnabled = true
                            loginBtn.text = "تسجيل الدخول"
                        }
                        return@launch
                    }
                    
                    val isAdmin = checkIfAdmin(userEmail)
                    
                    withContext(Dispatchers.Main) {
                        if (isAdmin) {
                            Toast.makeText(this@LoginActivity, "مرحباً بك أيها المسؤول", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@LoginActivity, AdminDashboardActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        } else {
                            Toast.makeText(this@LoginActivity, "تم تسجيل الدخول بنجاح", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }
                        finish()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMsg = when {
                        e.message?.contains("Invalid login credentials") == true -> "البريد الإلكتروني أو كلمة المرور غير صحيحة"
                        e.message?.contains("Email not confirmed") == true -> "البريد الإلكتروني غير مفعل، يرجى التحقق من بريدك"
                        else -> "حدث خطأ: ${e.message}"
                    }
                    Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                    loginBtn.isEnabled = true
                    loginBtn.text = "تسجيل الدخول"
                }
            }
        }
    }

    private suspend fun checkIfAdmin(userEmail: String): Boolean {
        return try {
            val response = SupabaseClient.client
                .from("admins")
                .select {
                    filter {
                        ilike("email", userEmail)
                    }
                }
            val data = response.data
            data != "[]" && data != "null" && data.isNotEmpty()
        } catch (e: Exception) {
            Log.e("AdminCheck", "Error: ${e.message}")
            false
        }
    }
}
