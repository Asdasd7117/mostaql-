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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            val code = codeInput.text.toString().trim()
            if (code.isEmpty() || code.length < 6) {
                Toast.makeText(this, "الرجاء إدخال رمز صحيح مكون من 6 أرقام", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            verifyResetCode(userEmail, code)
        }

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

    private fun sendResetPasswordCode(email: String) {
        sendCodeBtn.isEnabled = false
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                SupabaseClient.client.auth.resetPasswordForEmail(email = email)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ForgotPasswordActivity, "تم إرسال رمز التحقق إلى بريدك الإلكتروني", Toast.LENGTH_LONG).show()
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

    private fun verifyResetCode(email: String, code: String) {
        verifyCodeBtn.isEnabled = false
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                SupabaseClient.client.auth.verifySingleOtp(
                    type = OtpType.Email.RECOVERY,
                    email = email,
                    token = code
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ForgotPasswordActivity, "تم التحقق بنجاح، أدخل كلمة المرور الجديدة", Toast.LENGTH_SHORT).show()
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

    private fun updateUserPassword(newPass: String) {
        changePasswordBtn.isEnabled = false
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                SupabaseClient.client.auth.updateUser(
                    config = {
                        password = newPass
                    }
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ForgotPasswordActivity, "تم تغيير كلمة المرور بنجاح", Toast.LENGTH_LONG).show()
                    finish()
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
