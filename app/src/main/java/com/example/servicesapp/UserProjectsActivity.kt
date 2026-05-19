package com.example.servicesapp.projects

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.servicesapp.R
import com.example.servicesapp.SupabaseClient
import com.example.servicesapp.data.ProjectRepository
import com.example.servicesapp.models.Project
import com.example.servicesapp.profile.UserProfileActivity
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class UserProjectsActivity : AppCompatActivity() {

    private lateinit var projectsContainer: LinearLayout
    private lateinit var tvTitle: TextView
    private var userId: String? = null
    private var ownerName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // إنشاء الواجهة برمجياً لضمان الدقة والجمال
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F9FAFB")) // خلفية هادئة
        }

        // الهيدر العلوي بتصميم أنيق
        val headerCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 80, 40, 60)
            val gd = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Color.parseColor("#4F46E5"), Color.parseColor("#6366F1")))
            background = gd
        }

        tvTitle = TextView(this).apply {
            textSize = 24f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        headerCard.addView(tvTitle)
        mainLayout.addView(headerCard)

        val scroll = ScrollView(this)
        projectsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 40, 30, 40)
        }
        scroll.addView(projectsContainer)
        mainLayout.addView(scroll)

        setContentView(mainLayout)

        userId = intent.getStringExtra("userId")
        ownerName = intent.getStringExtra("ownerName")
        tvTitle.text = "مشاريع $ownerName"

        loadProjects()
    }

    private fun loadProjects() {
        val uid = userId ?: return
        lifecycleScope.launch {
            try {
                val projects = ProjectRepository.getUserProjects(uid)
                withContext(Dispatchers.Main) {
                    projectsContainer.removeAllViews()
                    if (projects.isEmpty()) {
                        showEmptyState()
                    } else {
                        projects.forEach { addModernProjectCard(it) }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@UserProjectsActivity, "خطأ في التحميل", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addModernProjectCard(project: Project) {
        val currentUserId = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(45, 45, 45, 45)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, 40)
            }
            
            val bg = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 40f
                setStroke(1, Color.parseColor("#E5E7EB"))
            }
            background = bg
            elevation = 10f
            
            setOnClickListener {
                val intent = Intent(this@UserProjectsActivity, ProjectDetailActivity::class.java).apply {
                    putExtra("projectId", project.id ?: 0)
                    putExtra("projectName", project.name)
                    putExtra("projectDescription", project.description)
                    putExtra("projectLanguage", project.language)
                    putExtra("projectOwnerId", project.userId)
                }
                startActivity(intent)
            }
        }

        // اسم المشروع
        card.addView(TextView(this).apply {
            text = project.name
            textSize = 20f
            setTextColor(Color.parseColor("#111827"))
            setTypeface(null, Typeface.BOLD)
        })

        // الوصف
        card.addView(TextView(this).apply {
            text = project.description
            textSize = 14f
            setTextColor(Color.parseColor("#6B7280"))
            setPadding(0, 15, 0, 25)
            maxLines = 2
        })

        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // تاج اللغة
        footer.addView(TextView(this).apply {
            text = "🔧 ${project.language}"
            textSize = 12f
            setTextColor(Color.parseColor("#4F46E5"))
            setPadding(25, 10, 25, 10)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#EEF2FF"))
                cornerRadius = 20f
            }
        })

        if (currentUserId == project.userId) {
            footer.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 0, 1f) })
            footer.addView(Button(this).apply {
                text = "حذف"
                textSize = 11f
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#EF4444"))
                    cornerRadius = 15f
                }
                setOnClickListener { showDeleteDialog(project.id ?: 0) }
            })
        }

        card.addView(footer)
        projectsContainer.addView(card)
    }

    private fun showEmptyState() {
        projectsContainer.addView(TextView(this).apply {
            text = "لا توجد مشاريع حالياً"
            gravity = Gravity.CENTER
            setPadding(0, 200, 0, 0)
        })
    }

    private fun showDeleteDialog(id: Int) {
        AlertDialog.Builder(this)
            .setTitle("حذف المشروع")
            .setMessage("هل أنت متأكد؟")
            .setPositiveButton("نعم") { _, _ -> performDelete(id) }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun performDelete(id: Int) {
        lifecycleScope.launch {
            val success = ProjectRepository.deleteProject(id, userId ?: "")
            if (success) loadProjects()
        }
    }
}
