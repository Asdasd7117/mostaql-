package com.example.servicesapp.auth

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.servicesapp.R
import com.example.servicesapp.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.reflect.full.declaredMemberFunctions

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var stepEmailLayout: LinearLayout
    private lateinit var emailInput: EditText
    private lateinit var sendCodeBtn: Button

    private lateinit var stepCodeLayout: LinearLayout
    private lateinit var codeInput: EditText
    private lateinit var verifyCodeBtn: Button

    private lateinit var stepPasswordLayout: LinearLayout
    private lateinit var newPasswordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var changePasswordBtn: Button

    private var userEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        stepEmailLayout = findViewById(R.id.stepEmailLayout)
        stepCodeLayout = findViewById(R.id.stepCodeLayout)
        stepPasswordLayout = findViewById(R.id.stepPasswordLayout)

        emailInput = findViewById(R.id.emailInput)
        sendCodeBtn = findViewById(R.id.sendCodeBtn)

        codeInput = findViewById(R.id.codeInput)
        verifyCodeBtn = findViewById(R.id.verifyCodeBtn)

        newPasswordInput = findViewById(R.id.newPasswordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        changePasswordBtn = findViewById(R.id.changePasswordBtn)
    }

    private fun setupClickListeners() {
        sendCodeBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "الرجاء إدخال البريد الإلكتروني", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            userEmail = email
            sendResetPasswordCode(email)
        }

        verifyCodeBtn.setOnClickListener {
            // عند الضغط على زر التحقق، سيقوم الكود بفحص المكتبة وطباعة الدوال فوراً
            printCorrectFunctions()
        }

        changePasswordBtn.setOnClickListener {
            finish()
        }
    }

    private fun sendResetPasswordCode(email: String) {
        sendCodeBtn.isEnabled = false
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                SupabaseClient.client.auth.resetPasswordForEmail(email = email)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ForgotPasswordActivity, "تم إرسال الرمز! انتقل للخطوة التالية", Toast.LENGTH_SHORT).show()
                    stepEmailLayout.visibility = View.GONE
                    stepCodeLayout.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ForgotPasswordActivity, "خطأ: ${e.message}", Toast.LENGTH_LONG).show()
                    sendCodeBtn.isEnabled = true
                }
            }
        }
    }

    private fun printCorrectFunctions() {
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                // الوصول إلى كلاس الـ Auth الخاص بـ Kotlin بنظام الفحص المتقدم
                val authClass = SupabaseClient.client.auth::class
                
                // جلب أسماء كافة الدوال التي تبدأ بكلمة verify أو تحتوي عليها
                val functionNames = authClass.declaredMemberFunctions
                    .map { it.name }
                    .filter { it.contains("verify", ignoreCase = true) || it.contains("otp", ignoreCase = true) }
                    .joinToString(", ")

                withContext(Dispatchers.Main) {
                    if (functionNames.isNotEmpty()) {
                        // طباعة الدوال الحقيقية الموجودة داخل الـ SDK الخاص بك
                        Toast.makeText(this@ForgotPasswordActivity, "الدوال المتوفرة: $functionNames", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@ForgotPasswordActivity, "لم يتم العثور على دوال تحتوي على verify", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ForgotPasswordActivity, "فشل الفحص: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
