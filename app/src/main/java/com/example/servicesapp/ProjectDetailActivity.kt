package com.example.servicesapp.projects

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.servicesapp.R
import com.example.servicesapp.SupabaseClient
import com.example.servicesapp.auth.LoginActivity
import com.example.servicesapp.chat.ChatActivity
import com.example.servicesapp.data.ChatRepository
import com.example.servicesapp.data.ReviewRepository
import com.example.servicesapp.profile.UserProfile
import com.google.android.material.textfield.TextInputEditText
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.graphics.Color

class ProjectDetailActivity : AppCompatActivity() {

    private lateinit var tvProjectName: TextView
    private lateinit var tvOwnerName: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvLanguage: TextView
    private lateinit var tvPreviewLink: TextView
    private lateinit var tvGithubLink: TextView
    private lateinit var btnOpenPreview: Button
    private lateinit var btnOpenGithub: Button
    private lateinit var btnContactOwner: Button
    private lateinit var tvWeakCount: TextView
    private lateinit var tvGoodCount: TextView
    private lateinit var tvExcellentCount: TextView
    private lateinit var btnRateWeak: Button
    private lateinit var btnRateGood: Button
    private lateinit var btnRateExcellent: Button
    private lateinit var etComment: TextInputEditText
    private lateinit var btnAddComment: Button
    private lateinit var commentsContainer: LinearLayout
    private lateinit var layoutPreview: LinearLayout
    private lateinit var layoutGithub: LinearLayout
    private lateinit var divider: View
    private lateinit var tvGithubLabel: TextView
    
    private var projectId: Int? = null
    private var projectOwnerId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (SupabaseClient.client.auth.currentSessionOrNull() == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_project_detail)

        projectId = intent.getIntExtra("projectId", -1).takeIf { it != -1 }
        val projectName = intent.getStringExtra("projectName") ?: "مشروع"
        val projectDesc = intent.getStringExtra("projectDescription") ?: ""
        val projectLanguage = intent.getStringExtra("projectLanguage") ?: ""
        projectOwnerId = intent.getStringExtra("projectOwnerId")

        initViews()
        loadProjectData(projectName, projectDesc, projectLanguage)
        setupClickListeners()
        loadComments()
    }

    private fun initViews() {
        tvProjectName = findViewById(R.id.tvProjectName)
        tvOwnerName = findViewById(R.id.tvOwnerName)
        tvDescription = findViewById(R.id.tvDescription)
        tvLanguage = findViewById(R.id.tvLanguage)
        tvPreviewLink = findViewById(R.id.tvPreviewLink)
        tvGithubLink = findViewById(R.id.tvGithubLink)
        btnOpenPreview = findViewById(R.id.btnOpenPreview)
        btnOpenGithub = findViewById(R.id.btnOpenGithub)
        btnContactOwner = findViewById(R.id.btnContactOwner)
        tvWeakCount = findViewById(R.id.tvWeakCount)
        tvGoodCount = findViewById(R.id.tvGoodCount)
        tvExcellentCount = findViewById(R.id.tvExcellentCount)
        btnRateWeak = findViewById(R.id.btnRateWeak)
        btnRateGood = findViewById(R.id.btnRateGood)
        btnRateExcellent = findViewById(R.id.btnRateExcellent)
        etComment = findViewById(R.id.etComment)
        btnAddComment = findViewById(R.id.btnAddComment)
        commentsContainer = findViewById(R.id.commentsContainer)
        layoutPreview = findViewById(R.id.layoutPreview)
        layoutGithub = findViewById(R.id.layoutGithub)
        divider = findViewById(R.id.divider)
        
        tvGithubLabel = layoutGithub.getChildAt(0) as TextView
    }

    private fun loadProjectData(name: String, desc: String, language: String) {
        tvProjectName.text = name
        tvDescription.text = desc
        tvLanguage.text = "🔧 $language"
        
        layoutPreview.visibility = View.VISIBLE
        layoutGithub.visibility = View.VISIBLE
        divider.visibility = View.VISIBLE
        tvGithubLabel.text = "💻 رابط GitHub:"
        btnOpenGithub.text = "فتح رابط GitHub"

        when (language) {
            "كتابة مقالات" -> {
                layoutPreview.visibility = View.GONE
                tvGithubLabel.text = "🔗 رابط المقالات:"
                btnOpenGithub.text = "فتح الرابط"
            }
            "تصاميم وشعارات" -> {
                layoutPreview.visibility = View.GONE
                tvGithubLabel.text = "🎨 رابط التصاميم:"
                btnOpenGithub.text = "عرض الأعمال"
            }
            "رفع تطبيقات أندرويد وأيفون" -> {
                layoutPreview.visibility = View.GONE
                layoutGithub.visibility = View.GONE
                divider.visibility = View.GONE
            }
        }

        loadProjectLinks()
        loadOwnerName()
        
        val id = projectId ?: return
        
        lifecycleScope.launch {
            try {
                val counts = ReviewRepository.getRatingCounts(id)
                tvWeakCount.text = "${counts["weak"] ?: 0} تقييم"
                tvGoodCount.text = "${counts["good"] ?: 0} تقييم"
                tvExcellentCount.text = "${counts["excellent"] ?: 0} تقييم"
            } catch (e: Exception) {
                tvWeakCount.text = "0 تقييم"
                tvGoodCount.text = "0 تقييم"
                tvExcellentCount.text = "0 تقييم"
            }
        }
    }

    private fun loadOwnerName() {
        val ownerId = projectOwnerId ?: run {
            tvOwnerName.text = "بواسطة: مستخدم"
            return
        }
        
        lifecycleScope.launch {
            try {
                val profiles = SupabaseClient.client
                    .from("user_profiles")
                    .select {
                        filter { 
                            eq("user_id", ownerId)
                        }
                    }
                    .decodeList<UserProfile>()
                
                val profile = profiles.firstOrNull()
                val ownerName = profile?.username ?: "مستخدم"
                
                val fullText = "بواسطة: $ownerName  [ زيارة الصفحة ]"
                val spannableString = SpannableString(fullText)
                val blueColor = Color.parseColor("#007BFF")
                val startIndex = fullText.indexOf("[ زيارة الصفحة ]")
                spannableString.setSpan(ForegroundColorSpan(blueColor), startIndex, fullText.length, 0)
                
                tvOwnerName.text = spannableString
                tvOwnerName.setOnClickListener {
                    val intentProjects = Intent(this@ProjectDetailActivity, UserProjectsActivity::class.java)
                    intentProjects.putExtra("userId", ownerId)
                    intentProjects.putExtra("ownerName", ownerName)
                    startActivity(intentProjects)
                }
                
            } catch (e: Exception) {
                tvOwnerName.text = "بواسطة: مستخدم"
            }
        }
    }

    private fun loadProjectLinks() {
        val id = projectId ?: return
        
        lifecycleScope.launch {
            try {
                val projects = SupabaseClient.client
                    .from("projects")
                    .select {
                        filter { eq("id", id) }
                    }
                    .decodeList<com.example.servicesapp.models.Project>()
                
                val project = projects.firstOrNull()
                
                val previewLink = project?.previewLink
                if (!previewLink.isNullOrEmpty()) {
                    tvPreviewLink.text = previewLink
                    btnOpenPreview.setOnClickListener {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(previewLink)))
                    }
                } else {
                    layoutPreview.visibility = View.GONE
                }
                
                val githubLink = project?.githubLink
                if (!githubLink.isNullOrEmpty()) {
                    tvGithubLink.text = githubLink
                    btnOpenGithub.setOnClickListener {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(githubLink)))
                    }
                } else {
                    layoutGithub.visibility = View.GONE
                }
            } catch (e: Exception) {
                layoutPreview.visibility = View.GONE
                layoutGithub.visibility = View.GONE
            }
        }
    }

    private fun checkUsernameAndExecute(action: () -> Unit) {
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
                    Toast.makeText(this@ProjectDetailActivity, "⚠️ يجب تعيين اسم مستخدم في ملفك الشخصي أولاً للقيام بهذا الإجراء", Toast.LENGTH_LONG).show()
                } else {
                    action()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProjectDetailActivity, "يرجى إكمال ملفك الشخصي أولاً", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        btnContactOwner.setOnClickListener {
            checkUsernameAndExecute {
                val ownerId = projectOwnerId
                val currentUserId = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id?.toString()
                
                if (ownerId == null) {
                    Toast.makeText(this, "خطأ: معرف المالك غير متوفر", Toast.LENGTH_SHORT).show()
                    return@checkUsernameAndExecute
                }
                
                if (currentUserId == null) {
                    Toast.makeText(this, "خطأ: يجب تسجيل الدخول", Toast.LENGTH_SHORT).show()
                    return@checkUsernameAndExecute
                }
                
                if (ownerId == currentUserId) {
                    Toast.makeText(this, "لا يمكنك مراسلة نفسك", Toast.LENGTH_SHORT).show()
                    return@checkUsernameAndExecute
                }
                
                lifecycleScope.launch {
                    try {
                        val convId = ChatRepository.getOrCreateConversation(
                            currentUserId, 
                            ownerId, 
                            projectId?.toLong()
                        )
                        
                        if (convId != null) {
                            val otherUsername = tvOwnerName.text.toString().replace("بواسطة: ", "").split("  ")[0]
                            
                            val intent = Intent(this@ProjectDetailActivity, ChatActivity::class.java).apply {
                                putExtra("conversationId", convId)
                                putExtra("otherUserId", ownerId)
                                putExtra("projectId", projectId)
                                putExtra("otherUsername", otherUsername)
                            }
                            startActivity(intent)
                        } else {
                            Toast.makeText(
                                this@ProjectDetailActivity, 
                                "فشل فتح المحادثة", 
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@ProjectDetailActivity, 
                            "خطأ: ${e.message}", 
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
        
        btnRateWeak.setOnClickListener { checkUsernameAndExecute { submitRating("weak") } }
        btnRateGood.setOnClickListener { checkUsernameAndExecute { submitRating("good") } }
        btnRateExcellent.setOnClickListener { checkUsernameAndExecute { submitRating("excellent") } }
        
        btnAddComment.setOnClickListener {
            checkUsernameAndExecute {
                val commentText = etComment.text.toString().trim()
                if (commentText.isNotEmpty()) {
                    addComment(commentText)
                } else {
                    Toast.makeText(this, "الرجاء إدخال تعليق", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun submitRating(rating: String) {
        if (SupabaseClient.client.auth.currentSessionOrNull() == null) {
            Toast.makeText(this, "يجب تسجيل الدخول أولاً للتقييم", Toast.LENGTH_SHORT).show()
            return
        }

        val id = projectId ?: return
        
        lifecycleScope.launch {
            try {
                val success = ReviewRepository.submitReview(id, rating)
                if (success) {
                    Toast.makeText(this@ProjectDetailActivity, "تم التقييم بنجاح", Toast.LENGTH_SHORT).show()
                    loadProjectData(
                        tvProjectName.text.toString(),
                        tvDescription.text.toString(),
                        tvLanguage.text.toString().replace("🔧 ", "")
                    )
                } else {
                    Toast.makeText(this@ProjectDetailActivity, "فشل التقييم أو قمت بالتقييم مسبقاً", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProjectDetailActivity, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addComment(commentText: String) {
        if (SupabaseClient.client.auth.currentSessionOrNull() == null) {
            Toast.makeText(this, "يجب تسجيل الدخول أولاً للتعليق", Toast.LENGTH_SHORT).show()
            return
        }

        val id = projectId ?: return
        
        lifecycleScope.launch {
            try {
                val success = ReviewRepository.addComment(id, commentText)
                if (success) {
                    etComment.setText("")
                    loadComments()
                    Toast.makeText(this@ProjectDetailActivity, "تم إضافة التعليق", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ProjectDetailActivity, "فشل إضافة التعليق", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProjectDetailActivity, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadComments() {
        val id = projectId ?: return
        
        lifecycleScope.launch {
            try {
                val comments = ReviewRepository.getProjectComments(id)
                commentsContainer.removeAllViews()
                
                if (comments.isEmpty()) {
                    val tvNoComments = TextView(this@ProjectDetailActivity).apply {
                        text = "لا توجد تعليقات بعد"
                        textSize = 14f
                        setTextColor(getColor(android.R.color.darker_gray))
                        setPadding(0, 20, 0, 20)
                        gravity = android.view.Gravity.CENTER
                    }
                    commentsContainer.addView(tvNoComments)
                } else {
                    comments.forEach { comment ->
                        val commentLayout = LinearLayout(this@ProjectDetailActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(16, 12, 16, 12)
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(0, 0, 0, 16)
                            }
                            setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
                        }

                        val tvCommentUser = TextView(this@ProjectDetailActivity).apply {
                            textSize = 13f
                            setTextColor(android.graphics.Color.GREEN)
                            setTypeface(null, android.graphics.Typeface.BOLD)
                            text = "جاري التحميل..."
                        }

                        lifecycleScope.launch {
                            try {
                                val profiles = SupabaseClient.client
                                    .from("user_profiles")
                                    .select { filter { eq("user_id", comment.userId) } }
                                    .decodeList<UserProfile>()
                                
                                val profile = profiles.firstOrNull()
                                val commentUserName = profile?.username ?: "مستخدم مجهول"
                                val cFullText = "$commentUserName  [ زيارة ]"
                                val cSpannable = SpannableString(cFullText)
                                val cBlueColor = Color.parseColor("#007BFF")
                                val cStartIndex = cFullText.indexOf("[ زيارة ]")
                                cSpannable.setSpan(ForegroundColorSpan(cBlueColor), cStartIndex, cFullText.length, 0)
                                
                                tvCommentUser.text = cSpannable
                                tvCommentUser.setOnClickListener {
                                    val intentProjects = Intent(this@ProjectDetailActivity, UserProjectsActivity::class.java)
                                    intentProjects.putExtra("userId", comment.userId)
                                    intentProjects.putExtra("ownerName", commentUserName)
                                    startActivity(intentProjects)
                                }
                            } catch (e: Exception) {
                                tvCommentUser.text = "مستخدم"
                            }
                        }

                        val tvCommentText = TextView(this@ProjectDetailActivity).apply {
                            text = comment.commentText
                            textSize = 15f
                            setTextColor(getColor(android.R.color.black))
                            setPadding(0, 4, 0, 0)
                        }

                        commentLayout.addView(tvCommentUser)
                        commentLayout.addView(tvCommentText)
                        commentsContainer.addView(commentLayout)
                    }
                }
            } catch (e: Exception) {
                val tvError = TextView(this@ProjectDetailActivity).apply {
                    text = "خطأ في تحميل التعليقات"
                    textSize = 14f
                    setTextColor(getColor(android.R.color.holo_red_dark))
                    setPadding(0, 20, 0, 20)
                    gravity = android.view.Gravity.CENTER
                }
                commentsContainer.addView(tvError)
            }
        }
    }
}
