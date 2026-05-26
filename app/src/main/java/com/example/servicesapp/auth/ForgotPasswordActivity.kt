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
import io.github.jan.supabase.gotrue.OtpType
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.user.UserUpdateBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ForgotPasswordActivity : AppCompatActivity() {

    // عناصر واجهة البريد الإلكتروني
    private lateinit var stepEmailLayout: LinearLayout
    private lateinit var emailInput: EditText
    private lateinit var sendCodeBtn: Button

    // عناصر واجهة رمز التحقق
    private lateinit var stepCodeLayout: LinearLayout
    private lateinit var codeInput: EditText
    private lateinit var verifyCodeBtn: Button

    // عناصر واجهة كلمة المرور الجديدة
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
        // ربط الحاويات (Layouts)
        stepEmailLayout = findViewById(R.id.stepEmailLayout)
        stepCodeLayout = findViewById(R.id.stepCodeLayout)
        stepPasswordLayout = findViewById(R.id.stepPasswordLayout)

        // ربط العناصر الداخلية
        emailInput = findViewById(R.id.emailInput)
        sendCodeBtn = findViewById(R.id.sendCodeBtn)

        codeInput = findViewById(R.id.codeInput)
        verifyCodeBtn = findViewById(R.id.verifyCodeBtn)

        newPasswordInput = findViewById(R.id.newPasswordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        changePasswordBtn = findViewById(R.id.changePasswordBtn)
    }

    private fun setupClickListeners() {
        // الخطوة 1: إرسال الرمز
        sendCodeBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "الرجاء إدخال البريد الإلكتروني", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            userEmail = email
            sendResetPasswordCode(email)
        }

        // الخطوة 2: التحقق من الرمز
        verifyCodeBtn.setOnClickListener {
            val code = codeInput.text.toString().trim()
            if (code.isEmpty() || code.length < 6) {
                Toast.makeText(this, "الرجاء إدخال رمز صحيح مكون من 6 أرقام", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            verifyResetCode(userEmail, code)
        }

        // الخطوة 3: تغيير كلمة المرور
        changePasswordBtn.setOnClickListener {
            val newPass = newPasswordInput.text.toString().trim()
            val confirmPass = confirmPasswordInput.text.toString().trim()

            if (newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "الرجاء ملء جميع الحقول", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass != confirmPass) {
                Toast.makeText(this, "كلمتا المرور غير متطابقتين", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass.length < 6) {
                Toast.makeText(this, "كلمة المرور يجب أن لا تقل عن 6 أحرف", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            updateUserPassword(newPass)
        }
    }

    // 1. دالة إرسال الرمز عبر Supabase
    private fun sendResetPasswordCode(email: String) {
        sendCodeBtn.isEnabled = false
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // إرسال رمز إعادة التعيين للبريد
                SupabaseClient.client.auth.resetPasswordForEmail(email = email)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ForgotPasswordActivity, "تم إرسال رمز التحقق إلى بريدك الإلكتروني", Toast.LENGTH_LONG).show()
                    // الانتقال للواجهة التالية
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

    // 2. دالة التحقق من الرمز (OTP) عبر Supabase
    private fun verifyResetCode(email: String, code: String) {
        verifyCodeBtn.isEnabled = false
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // التحقق من الرمز المكون من 6 أرقام ونوعه Recovery
                SupabaseClient.client.auth.verifyOtp(
                    type = OtpType.Email.RECOVERY,
                    email = email,
                    token = code
                )
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ForgotPasswordActivity, "تم التحقق بنجاح، أدخل كلمة المرور الجديدة", Toast.LENGTH_SHORT).show()
                    // الانتقال للواجهة الأخيرة
                    stepCodeLayout.visibility = View.GONE
                    stepPasswordLayout.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ForgotPasswordActivity, "الرمز غير صحيح أو منتهي الصلاحية", Toast.LENGTH_LONG).show()
                    verifyCodeBtn.isEnabled = true
                }
            }
        }
    }

    // 3. دالة تحديث كلمة المرور الجديدة في Supabase
    private fun updateUserPassword(newPass: String) {
        changePasswordBtn.isEnabled = false
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // تحديث بيانات المستخدم الحالي (الذي سجل الدخول تلقائياً بعد التحقق من الرمز)
                SupabaseClient.client.auth.updateUser {
                    password = newPass
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ForgotPasswordActivity, "تم تغيير كلمة المرور بنجاح", Toast.LENGTH_LONG).show()
                    finish() // العودة لصفحة تسجيل الدخول
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ForgotPasswordActivity, "فشل تغيير كلمة المرور: ${e.message}", Toast.LENGTH_LONG).show()
                    changePasswordBtn.isEnabled = true
                }
            }
        }
    }
}
