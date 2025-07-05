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
        
        // Set theme color only on the app bar (header)
        val themeColor = ThemeUtils.getCurrentThemeColor(this)
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.setBackgroundColor(themeColor)
        toolbar.setTitleTextColor(android.graphics.Color.WHITE)
        toolbar.navigationIcon?.setTint(android.graphics.Color.WHITE)
        setSupportActionBar(toolbar)

        val boldTitle = SpannableString("Settings")
        boldTitle.setSpan(StyleSpan(Typeface.BOLD), 0, boldTitle.length, 0)
        supportActionBar?.title = boldTitle
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_circle_white)
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