package com.example.diaryapp

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsMainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_main)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Settings"
        toolbar.setNavigationOnClickListener {
            finish()
        }

        val securityOption = findViewById<LinearLayout>(R.id.securityOption)
        securityOption.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
} 