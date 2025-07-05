package com.example.diaryapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.MessageDigest
import android.widget.TextView
import android.widget.VideoView
import android.net.Uri
import android.util.AttributeSet
import android.view.TextureView
import android.view.Surface
import android.media.MediaPlayer
import android.view.SurfaceHolder
import android.widget.FrameLayout
import android.view.View
import android.widget.ImageView

class AuthActivity : AppCompatActivity() {
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var submitButton: Button
    private lateinit var signupButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        val prefs = getEncryptedPrefs()
        val customBgUri = prefs.getString("login_bg_uri", null)
        val customBgIsImage = prefs.getBoolean("login_bg_is_image", false)

        val videoView = findViewById<VideoView>(R.id.loginVideoView)
        val imageView = findViewById<ImageView?>(R.id.loginBgImageView)
        if (customBgUri != null) {
            if (customBgIsImage && imageView != null) {
                imageView.setImageURI(Uri.parse(customBgUri))
                imageView.visibility = View.VISIBLE
                videoView.visibility = View.GONE
            } else {
                videoView.setVideoURI(Uri.parse(customBgUri))
                videoView.setOnPreparedListener { mp ->
                    mp.isLooping = true
                    mp.setVolume(0f, 0f)
                }
                videoView.start()
                videoView.visibility = View.VISIBLE
                imageView?.visibility = View.GONE
            }
        } else {
            // Default video
            videoView.setVideoURI(Uri.parse("android.resource://" + packageName + "/raw/login_page_into_anime"))
            videoView.setOnPreparedListener { mp ->
                mp.isLooping = true
                mp.setVolume(0f, 0f)
            }
            videoView.start()
            videoView.visibility = View.VISIBLE
            imageView?.visibility = View.GONE
        }

        usernameInput = findViewById(R.id.usernameInput)
        passwordInput = findViewById(R.id.passwordInput)
        submitButton = findViewById(R.id.submitButton)
        signupButton = findViewById(R.id.signupButton)

        val savedUsername = prefs.getString("username", null)
        val savedPassword = prefs.getString("password_hash", null)
        val savedName = prefs.getString("name", null)

        submitButton.text = "Sign In"
        signupButton.text = "Sign Up"
        signupButton.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }
        submitButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString()
            if (username == savedUsername && hash(password) == savedPassword) {
                goToMain(savedName ?: username)
            } else {
                Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun goToMain(name: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("user_name", name)
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
} 