package com.example.diaryapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        Handler(Looper.getMainLooper()).postDelayed({
            val prefs = EncryptedSharedPreferences.create(
                "diary_auth_prefs",
                MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                this,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            val username = prefs.getString("username", null)
            val password = prefs.getString("password_hash", null)
            if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
                startActivity(Intent(this, AuthActivity::class.java))
            } else {
                startActivity(Intent(this, MainActivity::class.java))
            }
            finish()
        }, 1500)
    }
} 