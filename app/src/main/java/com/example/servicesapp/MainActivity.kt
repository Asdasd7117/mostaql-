package com.example.servicesapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.example.servicesapp.auth.LoginActivity
import com.example.servicesapp.chat.ChatListActivity
import com.example.servicesapp.data.ProjectRepository
import com.example.servicesapp.models.Project
import com.example.servicesapp.projects.ProjectDetailActivity
import com.example.servicesapp.projects.UserProjectsActivity
import com.example.servicesapp.profile.UserProfile
import com.example.servicesapp.profile.UserProfileActivity
import com.google.android.material.navigation.NavigationView
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var projectsContainer: LinearLayout
    private lateinit var btnMyProjects: Button
    private lateinit var btnCategories: ImageView

    private var selectedCategory: String = "الكل"
    private var currentUserProfile: UserProfile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        projectsContainer = findViewById(R.id.projectsContainer)
        btnMyProjects = findViewById(R.id.btnMyProjects)
        btnCategories = findViewById(R.id.btnCategories)

        btnCategories.setOnClickListener { view ->
            showCategoriesMenu(view)
        }

        lifecycleScope.launch {
            SupabaseClient.client.auth.loadFromStorage()
            if (SupabaseClient.client.auth.currentSessionOrNull() == null) {
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
                return@launch
            }

            val session = SupabaseClient.client.auth.currentSessionOrNull()
            val email = session?.user?.email ?: "user@example.com"
            val userName = email.split("@").firstOrNull() ?: "مستخدم"
            
            updateNavigationView(email, userName)
            setupNavigation()
            loadProjectsByCategory(selectedCategory)
        }
    }

    private fun showCategoriesMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        
        popupMenu.menu.add("الكل")
        popupMenu.menu.add("مشاريع تطبيقات الاندرويد والويب")
        popupMenu.menu.add("مشاريع مواقع ويب")
        popupMenu.menu.add("كتابة مقالات")
        popupMenu.menu.add("تصاميم وشعارات")
        popupMenu.menu.add("رفع تطبيقات أندرويد")

        popupMenu.setOnMenuItemClickListener { item ->
            selectedCategory = item.title.toString()
            Toast.makeText(this, "تم اختيار: $selectedCategory", Toast.LENGTH_SHORT).show()
            loadProjectsByCategory(selectedCategory)
            true
        }
        popupMenu.show()
    }

    private fun updateNavigationView(email: String, userName: String) {
        val header = navigationView.getHeaderView(0)
        if (header == null) return

        val tvUserName = header.findViewById<TextView>(R.id.tvUserName)
        val tvUserEmail = header.findViewById<TextView>(R.id.tvUserEmail)

        lifecycleScope.launch {
            try {
                val session = SupabaseClient.client.auth.currentSessionOrNull()
                val userId = session?.user?.id
                
                var displayName = userName
                var displayEmail = email

                if (userId != null) {
                    val profiles = SupabaseClient.client
                        .from("user_profiles")
                        .select {
                            filter { eq("user_id", userId) }
                        }
                        .decodeList<UserProfile>()
                    
                    currentUserProfile = profiles.firstOrNull()
                    
                    if (currentUserProfile?.username != null && currentUserProfile!!.username!!.isNotBlank()) {
                        displayName = currentUserProfile!!.username!!
                    } else {
                        displayName = "يرجى إعداد اسم مستخدم ⚠️"
                    }
                    displayEmail = session.user?.email ?: email
                }
                
                withContext(Dispatchers.Main) {
                    tvUserName?.text = displayName
                    if (currentUserProfile?.username.isNullOrBlank()) {
                        tvUserName?.setTextColor(Color.RED)
                    } else {
                        tvUserName?.setTextColor(Color.BLACK) 
                    }
                    tvUserEmail?.text = displayEmail
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvUserName?.text = userName
                    tvUserEmail?.text = email
                }
            }
        }
    }

    private fun checkUsernameAndNavigate(destination: Class<*>, gravityToClose: Int, extras: (Intent) -> Unit = {}) {
        if (currentUserProfile?.username.isNullOrBlank()) {
            Toast.makeText(this, "⚠️ يجب تعيين اسم مستخدم في ملفك الشخصي أولاً!", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, UserProfileActivity::class.java))
        } else {
            val intent = Intent(this, destination)
            extras(intent)
            startActivity(intent)
        }
        drawerLayout.closeDrawer(gravityToClose)
    }

    private fun setupNavigation() {
        findViewById<ImageView>(R.id.btnMenu).setOnClickListener {
            Toast.makeText(this, "القائمة", Toast.LENGTH_SHORT).show()
            drawerLayout.openDrawer(GravityCompat.START)
        }

        findViewById<ImageView>(R.id.btnCategories).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navUsername, R.id.navProfile -> {
                    startActivity(Intent(this@MainActivity, UserProfileActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.navChats -> {
                    checkUsernameAndNavigate(ChatListActivity::class.java, GravityCompat.START)
                    true
                }
                R.id.navMyProjects -> {
                    checkUsernameAndNavigate(UserProjectsActivity::class.java, GravityCompat.START) { intent ->
                        val userId = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id?.toString()
                        intent.putExtra("userId", userId)
                        intent.putExtra("ownerName", currentUserProfile?.username ?: "المستخدم")
                    }
                    true
                }
                R.id.navAddProject -> {
                    checkUsernameAndNavigate(com.example.servicesapp.projects.AddProjectActivity::class.java, GravityCompat.START)
                    true
                }
                R.id.navLogout -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    lifecycleScope.launch {
                        SupabaseClient.client.auth.signOut()
                        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                        finish()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun loadProjectsByCategory(category: String) {
        lifecycleScope.launch {
            try {
                val allProjects = ProjectRepository.getAllProjects()
                
                val filteredProjects = if (category == "الكل") {
                    allProjects
                } else {
                    allProjects.filter { project -> 
                        val projectLang = project.language.trim().lowercase()
                        val targetCat = category.trim().lowercase()
                        
                        projectLang.contains(targetCat) || targetCat.contains(projectLang)
                    }
                }

                withContext(Dispatchers.Main) {
                    projectsContainer.removeAllViews()
                    if (filteredProjects.isEmpty()) {
                        projectsContainer.addView(TextView(this@MainActivity).apply {
                            text = "لا توجد مشاريع في قسم ($category) حالياً"
                            textSize = 16f
                            setTextColor(Color.parseColor("#6B7280"))
                            gravity = Gravity.CENTER
                            setPadding(0, 80, 0, 80)
                        })
                    } else {
                        var currentRow: LinearLayout? = null
                        
                        filteredProjects.forEachIndexed { index, project ->
                            if (index % 2 == 0) {
                                currentRow = LinearLayout(this@MainActivity).apply {
                                    orientation = LinearLayout.HORIZONTAL
                                    layoutParams = LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                    )
                                    weightSum = 2f
                                }
                                projectsContainer.addView(currentRow)
                            }
                            
                            val card = createProjectCard(project)
                            currentRow?.addView(card)
                            
                            if (index == filteredProjects.size - 1 && index % 2 == 0) {
                                currentRow?.addView(View(this@MainActivity).apply {
                                    layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
                                })
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "خطأ في التصفية: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun createProjectCard(project: Project): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 16, 20, 16)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(10, 10, 10, 10)
            }
            setBackgroundResource(R.drawable.project_card_background)
            elevation = 6f
            setOnClickListener {
                val intent = Intent(this@MainActivity, ProjectDetailActivity::class.java).apply {
                    putExtra("projectId", project.id ?: 0)
                    putExtra("projectName", project.name)
                    putExtra("projectDescription", project.description)
                    putExtra("projectLanguage", project.language)
                    putExtra("projectOwnerId", project.userId)
                }
                startActivity(intent)
            }
        }

        card.addView(TextView(this).apply {
            text = project.name
            textSize = 17f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            maxLines = 1
        })
        
        card.addView(TextView(this).apply {
            text = project.description
            textSize = 13f
            setTextColor(Color.argb(180, 255, 255, 255))
            setPadding(0, 6, 0, 6)
            maxLines = 2
        })
        
        card.addView(TextView(this).apply {
            text = "🔧 ${project.language}"
            textSize = 12f
            setTextColor(Color.YELLOW)
        })
        
        return card
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val session = SupabaseClient.client.auth.currentSessionOrNull()
            if (session != null) {
                updateNavigationView(session.user?.email ?: "", "")
                loadProjectsByCategory(selectedCategory)
            }
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
