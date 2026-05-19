package com.example.servicesapp.auth

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.servicesapp.R
import com.example.servicesapp.SupabaseClient
import com.example.servicesapp.home.HomeActivity
import io.github.jan.supabase.gotrue.OtpType
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VerifyActivity : AppCompatActivity() {

    private lateinit var code: EditText
    private lateinit var btn: Button
    private lateinit var resendBtn: Button
    private lateinit var emailText: TextView
    private lateinit var email: String
    private var resendAttempts = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify)

        email = intent.getStringExtra("email") ?: ""
        
        initViews()
        setupClickListeners()
        
        // عرض البريد (مخفي جزئياً)
        emailText.text = "تم إرسال الرمز إلى:\n${maskEmail(email)}"
    }

    private fun initViews() {
        code = findViewById(R.id.code)
        btn = findViewById(R.id.btnVerify)
        resendBtn = findViewById(R.id.btnResend)
        emailText = findViewById(R.id.emailText)
    }

    private fun setupClickListeners() {
        btn.setOnClickListener {
            val token = code.text.toString().trim()

            if (token.length != 6) {
                Toast.makeText(this, "⚠️ أدخل الرمز المكون من 6 أرقام", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            verifyCode(token)
        }

        resendBtn.setOnClickListener {
            if (resendAttempts >= 3) {
                Toast.makeText(this, "⚠️ تجاوزت الحد المسموح، حاول لاحقاً", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            resendVerificationCode()
            resendAttempts++
        }
    }

    private fun maskEmail(email: String): String {
        if (email.length < 5) return email
        val parts = email.split("@")
        if (parts.size != 2) return email
        val username = parts[0]
        val domain = parts[1]
        val maskedUsername = if (username.length > 2) {
            username[0] + "***" + username[username.length - 1]
        } else {
            username[0] + "***"
        }
        return "$maskedUsername@$domain"
    }

    private fun verifyCode(token: String) {
        btn.isEnabled = false
        btn.text = "جاري التحقق..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // ✅ التحقق من الرمز
                SupabaseClient.client.auth.verifyEmailOtp(
                    email = email,
                    token = token,
                    type = OtpType.Email.SIGNUP
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VerifyActivity, "✅ تم التحقق بنجاح!", Toast.LENGTH_SHORT).show()
                    
                    // الانتقال لصفحة تسجيل الدخول
                    val intent = Intent(this@VerifyActivity, LoginActivity::class.java)
                    intent.putExtra("email", email)
                    startActivity(intent)
                    finish()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMsg = when {
                        e.message?.contains("invalid", ignoreCase = true) == true -> 
                            "❌ الرمز غير صحيح\n\nتأكد من إدخال الرمز بشكل صحيح"
                        e.message?.contains("expired", ignoreCase = true) == true -> 
                            "❌ الرمز منتهي الصلاحية\n\nاضغط على \"إعادة إرسال الرمز\""
                        else -> "❌ حدث خطأ: ${e.message}"
                    }
                    Toast.makeText(this@VerifyActivity, errorMsg, Toast.LENGTH_LONG).show()
                    btn.isEnabled = true
                    btn.text = "تحقق"
                }
            }
        }
    }

    private fun resendVerificationCode() {
        resendBtn.isEnabled = false
        resendBtn.text = "جاري الإرسال..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // ✅ إعادة إرسال الرمز
                SupabaseClient.client.auth.signUpWith(io.github.jan.supabase.gotrue.providers.builtin.Email) {
                    this.email = email
                    this.password = "dummy" // لن يُستخدم، فقط لإعادة الإرسال
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@VerifyActivity,
                        "✅ تم إعادة إرسال الرمز\n\nتحقق من بريدك الإلكتروني",
                        Toast.LENGTH_SHORT
                    ).show()
                    resendBtn.text = "إعادة الإرسال (${3 - resendAttempts})"
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VerifyActivity, "❌ فشل إعادة الإرسال: ${e.message}", Toast.LENGTH_SHORT).show()
                    resendBtn.isEnabled = true
                    resendBtn.text = "إعادة إرسال الرمز"
                }
            }
        }
    }
}