package com.example.servicesapp.projects

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.servicesapp.R
import com.example.servicesapp.data.ProjectRepository
import com.example.servicesapp.models.Project
import kotlinx.coroutines.launch

class MyProjectsActivity : AppCompatActivity() {

    private lateinit var projectsContainer: LinearLayout
    private lateinit var tvNoProjects: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_projects)

        projectsContainer = findViewById(R.id.projectsContainer)
        tvNoProjects = findViewById(R.id.tvNoProjects)

        loadProjects()
    }

    private fun loadProjects() {
        lifecycleScope.launch {
            try {
                val projects = ProjectRepository.getAllProjects()
                
                if (projects.isEmpty()) {
                    tvNoProjects.visibility = View.VISIBLE
                    projectsContainer.visibility = View.GONE
                } else {
                    tvNoProjects.visibility = View.GONE
                    projectsContainer.visibility = View.VISIBLE
                    
                    projectsContainer.removeAllViews()
                    projects.forEach { project ->
                        addProjectCard(project)
                    }
                }
            } catch (e: Exception) {
                tvNoProjects.text = "❌ خطأ في تحميل المشاريع"
                tvNoProjects.visibility = View.VISIBLE
            }
        }
    }

    private fun addProjectCard(project: Project) {
        val cardView = LayoutInflater.from(this).inflate(R.layout.item_project_card, projectsContainer, false)
        
        cardView.findViewById<TextView>(R.id.tvProjectName).text = project.name
        cardView.findViewById<TextView>(R.id.tvProjectDesc).text = project.description
        cardView.findViewById<TextView>(R.id.tvLanguage).text = "🔧 ${project.language}"
        
        projectsContainer.addView(cardView)
    }
}