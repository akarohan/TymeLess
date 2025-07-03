package com.example.diaryapp

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.text.SpannableString
import android.text.style.StyleSpan
import android.graphics.Typeface

class SettingsMainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_main)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val boldTitle = SpannableString("Settings")
        boldTitle.setSpan(StyleSpan(Typeface.BOLD), 0, boldTitle.length, 0)
        supportActionBar?.title = boldTitle
        toolbar.setTitleTextColor(android.graphics.Color.BLACK)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back)
        toolbar.navigationIcon?.setTintList(null)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        val securityOption = findViewById<LinearLayout>(R.id.securityOption)
        securityOption.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
} 