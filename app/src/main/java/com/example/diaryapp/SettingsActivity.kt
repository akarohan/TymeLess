package com.example.diaryapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.MessageDigest
import android.net.Uri
import android.content.Intent
import android.provider.MediaStore
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class SettingsActivity : AppCompatActivity() {
    private lateinit var nameInput: EditText
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back)
        toolbar.navigationIcon?.setTintList(null)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        nameInput = findViewById(R.id.nameInput)
        usernameInput = findViewById(R.id.usernameInput)
        passwordInput = findViewById(R.id.passwordInput)
        saveButton = findViewById(R.id.saveButton)

        val prefs = getEncryptedPrefs()
        nameInput.setText(prefs.getString("name", ""))
        usernameInput.setText(prefs.getString("username", ""))
        // Log loaded values
        val loadedName = prefs.getString("name", "")
        val loadedUsername = prefs.getString("username", "")
        Log.d("SettingsLoad", "Loaded name: $loadedName, username: $loadedUsername")

        saveButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString()
            if (name.isEmpty() || username.isEmpty()) {
                Toast.makeText(this, "Name and username cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val editor = prefs.edit()
            editor.putString("name", name)
            editor.putString("username", username)
            if (password.isNotEmpty()) {
                editor.putString("password_hash", hash(password))
            }
            editor.apply()
            Log.d("SettingsSave", "Saved name: $name, username: $username")
            Toast.makeText(this, "Saved: $name, $username", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun getEncryptedPrefs() = EncryptedSharedPreferences.create(
        "diary_auth_prefs",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        this,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun hash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
} 