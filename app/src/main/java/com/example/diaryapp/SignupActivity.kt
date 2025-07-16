package com.example.diaryapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.MessageDigest

class SignupActivity : AppCompatActivity() {
    private lateinit var nameInput: EditText
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var signupButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        nameInput = findViewById(R.id.nameInput)
        usernameInput = findViewById(R.id.usernameInput)
        passwordInput = findViewById(R.id.passwordInput)
        signupButton = findViewById(R.id.signupButton)

        signupButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString()
            if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val prefs = getEncryptedPrefs()
            prefs.edit().putString("name", name)
                .putString("username", username)
                .putString("password_hash", hash(password)).apply()
            Toast.makeText(this, "Registration successful! Please log in.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun goToLogin() {
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
        finish()
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

    private fun checkForAccountBackupAndPrompt(username: String) {
        val backupDir = filesDir.resolve("backups")
        if (backupDir.exists()) {
            val backupFiles = backupDir.listFiles { file -> file.name.endsWith(".zip") }
            if (!backupFiles.isNullOrEmpty()) {
                for (zipFile in backupFiles) {
                    // Look for notes.json inside the zip
                    val tempDir = cacheDir.resolve("backup_check")
                    tempDir.mkdirs()
                    try {
                        java.util.zip.ZipFile(zipFile).use { zip ->
                            val entry = zip.getEntry("notes.json")
                            if (entry != null) {
                                val input = zip.getInputStream(entry)
                                val notesJson = input.bufferedReader().use { it.readText() }
                                val obj = org.json.JSONObject(notesJson)
                                val backupUserKey = obj.optString("user_key", null)
                                val backupUsername = obj.optString("username", null)
                                val userKey = getEncryptedPrefs().getString("user_key", null)
                                if ((backupUsername == username) || (userKey != null && backupUserKey == userKey)) {
                                    // Always route to restore page
                                    val intent = Intent(this, RestoreDataActivity::class.java)
                                    intent.putExtra("backup_zip_path", zipFile.absolutePath)
                                    startActivity(intent)
                                    finish()
                                    return
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
        Toast.makeText(this, "Registration successful! Please log in.", Toast.LENGTH_SHORT).show()
        goToLogin()
    }
} 