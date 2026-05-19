package com.example.servicesapp.home

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.servicesapp.R
import com.example.servicesapp.SupabaseClient
import com.example.servicesapp.utils.DB
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.*

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)

        val textView = findViewById<TextView>(R.id.textView)

        CoroutineScope(Dispatchers.IO).launch {
            try {

                val data = SupabaseClient.client
                    .from(DB.SERVICES)
                    .select()
                    .decodeList<Map<String, Any>>() // مؤقتًا بدون Model

                withContext(Dispatchers.Main) {
                    textView.text = data.toString()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    textView.text = "Error: ${e.message}"
                }
                e.printStackTrace()
            }
        }
    }
}