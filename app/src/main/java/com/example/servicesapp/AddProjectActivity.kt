package com.example.servicesapp.projects

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.servicesapp.R
import com.example.servicesapp.SupabaseClient
import com.example.servicesapp.data.ProjectRepository
import com.example.servicesapp.models.Project
import com.example.servicesapp.profile.UserProfile
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class AddProjectActivity : AppCompatActivity() {

    private lateinit var spinnerCategory: AutoCompleteTextView
    private lateinit var etProjectName: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var etLanguage: TextInputEditText
    private lateinit var etPreviewLink: TextInputEditText
    private lateinit var etGithubLink: TextInputEditText
    
    private lateinit var layoutLanguage: TextInputLayout
    private lateinit var layoutPreviewLink: TextInputLayout
    private lateinit var layoutGithubLink: TextInputLayout
    private lateinit var btnAdd: Button

    private val categories = arrayOf(
        "مشاريع تطبيقات اندرويد وايفون",
        "مشاريع مواقع ويب",
        "كتابة مقالات",
        "تصاميم وشعارات",
        "رفع تطبيقات أندرويد وأيفون"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_project)

        spinnerCategory = findViewById(R.id.spinnerCategory)
        etProjectName = findViewById(R.id.etProjectName)
        etDescription = findViewById(R.id.etDescription)
        etLanguage = findViewById(R.id.etLanguage)
        etPreviewLink = findViewById(R.id.etPreviewLink)
        etGithubLink = findViewById(R.id.etGithubLink)

        layoutLanguage = findViewById(R.id.layoutLanguage)
        layoutPreviewLink = findViewById(R.id.layoutPreviewLink)
        layoutGithubLink = findViewById(R.id.layoutGithubLink)
        btnAdd = findViewById(R.id.btnAdd)

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        spinnerCategory.setAdapter(adapter)

        spinnerCategory.setOnItemClickListener { _, _, position, _ ->
            updateUI(categories[position])
        }

        btnAdd.setOnClickListener {
            val name = etProjectName.text.toString().trim()
            val desc = etDescription.text.toString().trim()
            val category = spinnerCategory.text.toString()
            val lang = if (layoutLanguage.visibility == View.VISIBLE) etLanguage.text.toString().trim() else category
            val preview = etPreviewLink.text.toString().trim()
            val github = etGithubLink.text.toString().trim()

            if (name.isEmpty() || category.isEmpty() || desc.isEmpty()) {
                Toast.makeText(this, "يرجى إكمال البيانات الأساسية", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            checkUsernameAndAdd(name, desc, lang, preview, github)
        }
    }

    private fun updateUI(category: String) {
        layoutLanguage.visibility = View.VISIBLE
        layoutPreviewLink.visibility = View.VISIBLE
        layoutGithubLink.visibility = View.VISIBLE
        
        when (category) {
            "كتابة مقالات" -> {
                layoutLanguage.visibility = View.GONE
                layoutPreviewLink.visibility = View.GONE
                layoutGithubLink.hint = "أضف رابط أو روابط لمقالات لك"
            }
            "تصاميم وشعارات" -> {
                layoutLanguage.visibility = View.GONE
                layoutPreviewLink.visibility = View.GONE
                layoutGithubLink.hint = "أضف رابط أو روابط من صفحات شعارات أو تصاميم خاصة بك"
            }
            "رفع تطبيقات أندرويد وأيفون" -> {
                layoutLanguage.visibility = View.GONE
                layoutPreviewLink.visibility = View.GONE
                layoutGithubLink.visibility = View.GONE
            }
            else -> {
                layoutGithubLink.hint = "رابط GitHub (اختياري)"
            }
        }
    }

    private fun checkUsernameAndAdd(name: String, desc: String, language: String, preview: String, github: String) {
        btnAdd.isEnabled = false
        btnAdd.text = "جاري التحقق..."

        lifecycleScope.launch {
            try {
                val userId = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id ?: ""
                
                val profile = SupabaseClient.client
                    .from("user_profiles")
                    .select {
                        filter { eq("user_id", userId) }
                    }
                    .decodeSingleOrNull<UserProfile>()

                if (profile?.username.isNullOrBlank()) {
                    Toast.makeText(this@AddProjectActivity, "⚠️ يجب تعيين اسم مستخدم في ملفك الشخصي أولاً قبل إضافة أي مشروع", Toast.LENGTH_LONG).show()
                    btnAdd.isEnabled = true
                    btnAdd.text = "إضافة المشروع"
                } else {
                    addProject(name, desc, language, preview, github)
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddProjectActivity, "يرجى إكمال ملفك الشخصي أولاً", Toast.LENGTH_SHORT).show()
                btnAdd.isEnabled = true
                btnAdd.text = "إضافة المشروع"
            }
        }
    }

    private fun addProject(name: String, desc: String, language: String, preview: String, github: String) {
        btnAdd.text = "جاري الإضافة..."

        lifecycleScope.launch {
            try {
                val session = SupabaseClient.client.auth.currentSessionOrNull()
                val userId = session?.user?.id?.toString() ?: run {
                    Toast.makeText(this@AddProjectActivity, "❌ يجب تسجيل الدخول", Toast.LENGTH_SHORT).show()
                    btnAdd.isEnabled = true
                    btnAdd.text = "إضافة المشروع"
                    return@launch
                }

                val project = Project(
                    name = name,
                    description = desc,
                    language = language,
                    previewLink = preview,
                    githubLink = github,
                    userId = userId
                )

                ProjectRepository.addProject(project)

                Toast.makeText(this@AddProjectActivity, "✅ تم إضافة المشروع بنجاح", Toast.LENGTH_SHORT).show()
                finish()

            } catch (e: Exception) {
                Toast.makeText(this@AddProjectActivity, "❌ خطأ: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                btnAdd.isEnabled = true
                btnAdd.text = "إضافة المشروع"
            }
        }
    }
}
