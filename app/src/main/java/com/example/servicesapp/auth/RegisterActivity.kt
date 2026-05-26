package com.example.servicesapp.auth

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.servicesapp.R
import com.example.servicesapp.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class RegisterActivity : AppCompatActivity() {

    private lateinit var fullName: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var confirmPassword: EditText
    private lateinit var registerBtn: Button
    private lateinit var loginBtn: Button
    private lateinit var titleText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        titleText = findViewById(R.id.titleText)
        fullName = findViewById(R.id.fullName)
        email = findViewById(R.id.email)
        password = findViewById(R.id.password)
        confirmPassword = findViewById(R.id.confirmPassword)
        registerBtn = findViewById(R.id.registerBtn)
        loginBtn = findViewById(R.id.loginBtn)
    }

    private fun setupClickListeners() {
        registerBtn.setOnClickListener {
            val name = fullName.text.toString().trim()
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()
            val confirmPass = confirmPassword.text.toString().trim()

            if (!validateInputs(name, emailText, passwordText, confirmPass)) {
                return@setOnClickListener
            }

            registerUser(name, emailText, passwordText)
        }

        loginBtn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun validateInputs(name: String, email: String, password: String, confirmPass: String): Boolean {
        val allowedDomains = listOf("gmail.com", "hotmail.com")
        val emailDomain = if (email.contains("@")) email.substringAfter("@").lowercase() else ""

        when {
            name.isEmpty() -> {
                Toast.makeText(this, "الرجاء إدخال الاسم الكامل", Toast.LENGTH_SHORT).show()
                return false
            }
            email.isEmpty() -> {
                Toast.makeText(this, "الرجاء إدخال البريد الإلكتروني", Toast.LENGTH_SHORT).show()
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                Toast.makeText(this, "البريد الإلكتروني غير صحيح", Toast.LENGTH_SHORT).show()
                return false
            }
            !allowedDomains.contains(emailDomain) -> {
                Toast.makeText(this, "يسمح فقط بحسابات Gmail و Hotmail", Toast.LENGTH_SHORT).show()
                return false
            }
            password.isEmpty() -> {
                Toast.makeText(this, "الرجاء إدخال كلمة المرور", Toast.LENGTH_SHORT).show()
                return false
            }
            password.length < 6 -> {
                Toast.makeText(this, "كلمة المرور يجب أن تكون 6 أحرف على الأقل", Toast.LENGTH_SHORT).show()
                return false
            }
            password != confirmPass -> {
                Toast.makeText(this, "كلمتا المرور غير متطابقتين", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    private fun getDeviceUniqueID(): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
    }

    private suspend fun checkRegistrationEligibility(deviceId: String): Boolean {
        return try {
            val response = SupabaseClient.client.postgrest.rpc("can_register", buildJsonObject {
                put("p_device_id", deviceId)
            })
            response.data.toString().trim().replace("\"", "").toBoolean()
        } catch (e: Exception) {
            false
        }
    }

    private fun registerUser(name: String, email: String, password: String) {
        registerBtn.isEnabled = false
        registerBtn.text = "جاري إنشاء الحساب..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val deviceId = getDeviceUniqueID()
                val canRegister = checkRegistrationEligibility(deviceId)

                if (!canRegister) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@RegisterActivity, "⚠️ عذراً، لا يمكنك إنشاء أكثر من حساب خلال 48 ساعة من نفس الجهاز.", Toast.LENGTH_LONG).show()
                        registerBtn.isEnabled = true
                        registerBtn.text = "إنشاء حساب"
                    }
                    return@launch
                }

                SupabaseClient.client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }

                delay(1000)
                
                val userId = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id?.toString()
                
                if (userId != null) {
                    try {
                        SupabaseClient.client
                            .from("user_profiles")
                            .insert(
                                buildJsonObject {
                                    put("user_id", userId)
                                    put("username", name.lowercase().trim())
                                    put("country", "غير محدد")
                                    put("username_changed", false)
                                }
                            )

                        SupabaseClient.client.postgrest.rpc("register_device", buildJsonObject {
                            put("p_device_id", deviceId)
                        })
                        
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@RegisterActivity,
                        "✅ تم إنشاء الحساب بنجاح!\n\n📧 تم إرسال رمز التحقق إلى بريدك الإلكتروني",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    val intent = Intent(this@RegisterActivity, VerifyActivity::class.java)
                    intent.putExtra("email", email)
                    intent.putExtra("is_new_signup", true)
                    startActivity(intent)
                    finish()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMsg = when {
                        e.message?.contains("User already registered", ignoreCase = true) == true -> 
                            "❌ هذا البريد الإلكتروني مسجل مسبقاً"
                        e.message?.contains("Weak password", ignoreCase = true) == true -> 
                            "❌ كلمة المرور ضعيفة جداً"
                        else -> "❌ حدث خطأ: ${e.message}"
                    }
                    Toast.makeText(this@RegisterActivity, errorMsg, Toast.LENGTH_LONG).show()
                    registerBtn.isEnabled = true
                    registerBtn.text = "إنشاء حساب"
                }
            }
        }
    }
}
