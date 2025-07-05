package com.example.diaryapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.yalantis.ucrop.UCrop
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class LoginPageCustomizeActivity : AppCompatActivity() {
    private lateinit var pickMediaButton: Button
    private lateinit var saveButton: Button
    private lateinit var previewImage: ImageView
    private lateinit var previewVideo: VideoView
    private var selectedUri: Uri? = null
    private var isImage: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_page_customize)

        pickMediaButton = findViewById(R.id.pickMediaButton)
        saveButton = findViewById(R.id.saveButton)
        previewImage = findViewById(R.id.previewImage)
        previewVideo = findViewById(R.id.previewVideo)

        pickMediaButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/* video/*"
            intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
            startActivityForResult(Intent.createChooser(intent, "Select Media"), 1001)
        }

        saveButton.setOnClickListener {
            if (selectedUri != null) {
                val prefs = getEncryptedPrefs()
                prefs.edit().putString("login_bg_uri", selectedUri.toString())
                    .putBoolean("login_bg_is_image", isImage)
                    .apply()
                Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Please select a media file first.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == 1001) {
            val uri = data?.data ?: return
            val type = contentResolver.getType(uri) ?: ""
            if (type.startsWith("image")) {
                // Launch cropper with aspect ratio matching the device's screen
                val destUri = Uri.fromFile(File(cacheDir, "cropped_login_bg.jpg"))
                val displayMetrics = resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels
                val screenHeight = displayMetrics.heightPixels
                UCrop.of(uri, destUri)
                    .withAspectRatio(screenWidth.toFloat(), screenHeight.toFloat())
                    .withMaxResultSize(screenWidth, screenHeight)
                    .start(this)
            } else if (type.startsWith("video")) {
                showVideo(uri)
            }
        } else if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            val resultUri = UCrop.getOutput(data!!)
            if (resultUri != null) {
                showImage(resultUri)
            }
        }
    }

    private fun showImage(uri: Uri) {
        previewImage.setImageURI(uri)
        previewImage.visibility = ImageView.VISIBLE
        previewVideo.visibility = VideoView.GONE
        selectedUri = uri
        isImage = true
    }

    private fun showVideo(uri: Uri) {
        // Copy video to internal storage for reliable playback
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val outFile = File(filesDir, "login_bg_video.mp4")
        val outputStream = FileOutputStream(outFile)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        val fileUri = Uri.fromFile(outFile)
        previewVideo.setVideoURI(fileUri)
        previewVideo.setOnPreparedListener { mp ->
            mp.isLooping = true
            mp.setVolume(0f, 0f)
        }
        previewVideo.start()
        previewVideo.visibility = VideoView.VISIBLE
        previewImage.visibility = ImageView.GONE
        selectedUri = fileUri
        isImage = false
    }

    private fun getEncryptedPrefs() = EncryptedSharedPreferences.create(
        "diary_auth_prefs",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        this,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
} 