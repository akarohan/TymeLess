package com.example.diaryapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.MessageDigest

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
            Toast.makeText(this, "Details updated", Toast.LENGTH_SHORT).show()
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
} 