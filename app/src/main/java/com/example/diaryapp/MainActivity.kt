package com.example.diaryapp

import android.os.Bundle
import android.view.Menu
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.diaryapp.databinding.ActivityMainBinding
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import jp.wasabeef.richeditor.RichEditor
import com.google.android.material.datepicker.MaterialDatePicker
import java.util.*
import kotlinx.coroutines.*
import androidx.lifecycle.lifecycleScope
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import android.view.LayoutInflater
import android.content.Intent
import android.widget.Toast
import android.util.Log
import android.widget.LinearLayout
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import android.widget.Button
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileOutputStream
import android.provider.MediaStore
import android.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.yalantis.ucrop.UCrop
import androidx.core.view.GravityCompat

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private var selectedDate: Long = System.currentTimeMillis()
    private lateinit var db: DiaryDatabase
    private lateinit var pickProfileImageLauncher: androidx.activity.result.ActivityResultLauncher<String>
    private lateinit var takeProfileImageLauncher: androidx.activity.result.ActivityResultLauncher<android.net.Uri>
    private var cameraImageUri: android.net.Uri? = null
    private val CAMERA_PERMISSION_REQUEST_CODE = 1001
    private lateinit var cropImageLauncher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // setSupportActionBar(binding.appBarMain.toolbar) // keep this to use the toolbar

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = findViewById(R.id.nav_view)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        // appBarConfiguration = AppBarConfiguration(
        //     setOf(
        //         R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
        //     ), drawerLayout
        // )
        // setupActionBarWithNavController(navController, appBarConfiguration)

        db = DiaryDatabase.getDatabase(this)

        // Now safe to call updateDrawerHeader
        updateDrawerHeader()
        updateDateText()

        // Set version and built by in the footer (already included in layout)
        val versionTextView = findViewById<TextView>(R.id.navAppVersion)
        val builtByTextView = findViewById<TextView>(R.id.navBuiltBy)
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        versionTextView?.text = "Version $versionName"
        builtByTextView?.text = "Built by Rohan"

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    drawerLayout.closeDrawers()
                    findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.nav_home)
                    true
                }
                R.id.nav_gallery -> {
                    drawerLayout.closeDrawers()
                    findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.nav_gallery)
                    true
                }
                R.id.nav_slideshow -> {
                    drawerLayout.closeDrawers()
                    findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.nav_slideshow)
                    true
                }
                R.id.nav_settings -> {
                    drawerLayout.closeDrawers()
                    Log.d("DrawerTest", "Settings tapped")
                    Toast.makeText(this, "Opening Settings", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, SettingsMainActivity::class.java))
                    true
                }
                else -> false
            }
        }

        val headerView = navView.getHeaderView(0)
        val profileImageView = headerView.findViewById<ImageView>(R.id.imageView)
        profileImageView?.setOnClickListener {
            val options = arrayOf("Choose a Picture", "Take a Picture", "Remove Picture")
            AlertDialog.Builder(this)
                .setTitle("Profile Picture")
                .setItems(options) { dialog, which ->
                    when (which) {
                        0 -> pickProfileImageLauncher.launch("image/*")
                        1 -> {
                            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
                                return@setItems
                            }
                            launchCameraForProfileImage()
                        }
                        2 -> removeProfileImage()
                    }
                }
                .show()
        }

        // Update toolbar profile icon as well
        val prefsToolbar = getEncryptedPrefs()
        val profilePicUri = prefsToolbar.getString("profile_pic_uri", null)
        if (profilePicUri != null && profileImageView != null) {
            val uri = if (profilePicUri.startsWith("/")) {
                android.net.Uri.fromFile(java.io.File(profilePicUri))
            } else {
                android.net.Uri.parse(profilePicUri)
            }
            Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.ic_user_placeholder)
                .error(R.drawable.ic_user_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(profileImageView)
        } else if (profileImageView != null) {
            profileImageView.setImageResource(R.drawable.ic_user_placeholder)
        }

        // Set greeting message below the logo based on time and user name
        val greetingTextView = binding.appBarMain.toolbar.findViewById<TextView>(R.id.greeting)
        val prefsGreeting = getEncryptedPrefs()
        val userName = prefsGreeting.getString("name", "User") ?: "User"
        greetingTextView?.text = getGreetingMessage(userName)

        cropImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val resultUri = UCrop.getOutput(data!!)
                if (resultUri != null) {
                    saveProfileImageFromUri(resultUri)
                }
            }
        }
        pickProfileImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                launchCropper(it)
            }
        }
        takeProfileImageLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            val file = File(filesDir, "profile_image.jpg")
            if (success && file.exists() && file.length() > 0) {
                launchCropper(android.net.Uri.fromFile(file))
            } else {
                Toast.makeText(this, "Failed to save photo. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up toolbar profile image to open right-side navigation drawer when tapped
        val toolbarProfileImageView = binding.appBarMain.toolbar.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.profileImageView)
        toolbarProfileImageView?.setOnClickListener {
            binding.drawerLayout.openDrawer(androidx.core.view.GravityCompat.END)
        }
    }

    private fun updateDrawerHeader() {
        val navView: com.google.android.material.navigation.NavigationView = findViewById(R.id.nav_view)
        val headerView = navView.getHeaderView(0)
        if (headerView == null) return
        val profileImageView = headerView.findViewById<ImageView>(R.id.imageView)
        val nameTextView = headerView.findViewById<TextView>(R.id.navUserName)
        val usernameTextView = headerView.findViewById<TextView>(R.id.navUserUsername)
        val prefs = EncryptedSharedPreferences.create(
            "diary_auth_prefs",
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val loadedName = prefs.getString("name", "User")
        val loadedUsername = prefs.getString("username", "username")
        nameTextView?.text = loadedName
        usernameTextView?.text = loadedUsername
        val profilePicUri = prefs.getString("profile_pic_uri", null)
        Log.d("DrawerHeader", "profilePicUri: $profilePicUri")
        if (profilePicUri != null && profileImageView != null) {
            val uri = if (profilePicUri.startsWith("/")) {
                android.net.Uri.fromFile(java.io.File(profilePicUri))
            } else {
                android.net.Uri.parse(profilePicUri)
            }
            Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.ic_user_placeholder)
                .error(R.drawable.ic_user_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(profileImageView)
        } else if (profileImageView != null) {
            profileImageView.setImageResource(R.drawable.ic_user_placeholder)
        }
    }

    private fun updateDateText() {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
        val text = android.text.format.DateFormat.format("yyyy-MM-dd", cal)
        // dateTextView.text = text
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onPause() {
        super.onPause()
        // TODO: Implement save logic
    }

    override fun onResume() {
        super.onResume()
        updateDrawerHeader()
        // Update toolbar profile image on resume
        val toolbarProfileImageView = binding.appBarMain.toolbar.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.profileImageView)
        val prefsToolbar = getEncryptedPrefs()
        val profilePicUri = prefsToolbar.getString("profile_pic_uri", null)
        if (profilePicUri != null && toolbarProfileImageView != null) {
            val uri = if (profilePicUri.startsWith("/")) {
                android.net.Uri.fromFile(java.io.File(profilePicUri))
            } else {
                android.net.Uri.parse(profilePicUri)
            }
            Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.ic_user_placeholder)
                .error(R.drawable.ic_user_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(toolbarProfileImageView)
        } else if (toolbarProfileImageView != null) {
            toolbarProfileImageView.setImageResource(R.drawable.ic_user_placeholder)
        }
        // Update greeting message on resume
        val greetingTextView = binding.appBarMain.toolbar.findViewById<TextView>(R.id.greeting)
        val prefsGreeting = getEncryptedPrefs()
        val userName = prefsGreeting.getString("name", "User") ?: "User"
        greetingTextView?.text = getGreetingMessage(userName)
    }

    private fun launchCameraForProfileImage() {
        val file = File(filesDir, "profile_image.jpg")
        cameraImageUri = androidx.core.content.FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            file
        )
        takeProfileImageLauncher.launch(cameraImageUri)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchCameraForProfileImage()
        }
    }

    private fun launchCropper(sourceUri: Uri) {
        val destUri = android.net.Uri.fromFile(File(filesDir, "profile_image_cropped.jpg"))
        val uCrop = UCrop.of(sourceUri, destUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(512, 512)
        cropImageLauncher.launch(uCrop.getIntent(this))
    }

    private fun saveProfileImageFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(filesDir, "profile_image.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            // Save file path in preferences
            val prefs = getEncryptedPrefs()
            prefs.edit().putString("profile_pic_uri", file.absolutePath).apply()
            updateDrawerHeader()

            // Update toolbar/home header profile image
            val toolbarProfileImageView = binding.appBarMain.toolbar.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.profileImageView)
            val profilePicUri = prefs.getString("profile_pic_uri", null)
            if (profilePicUri != null && toolbarProfileImageView != null) {
                val uri = if (profilePicUri.startsWith("/")) {
                    android.net.Uri.fromFile(java.io.File(profilePicUri))
                } else {
                    android.net.Uri.parse(profilePicUri)
                }
                Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.ic_user_placeholder)
                    .error(R.drawable.ic_user_placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(toolbarProfileImageView)
            } else if (toolbarProfileImageView != null) {
                toolbarProfileImageView.setImageResource(R.drawable.ic_user_placeholder)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeProfileImage() {
        val file = File(filesDir, "profile_image.jpg")
        if (file.exists()) file.delete()
        val prefs = getEncryptedPrefs()
        prefs.edit().remove("profile_pic_uri").apply()
        updateDrawerHeader()
    }

    private fun getEncryptedPrefs() = EncryptedSharedPreferences.create(
        "diary_auth_prefs",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        this,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun getGreetingMessage(name: String): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when {
            hour in 5..11 -> "Good Morning, $name"
            hour in 12..16 -> "Good Afternoon, $name"
            else -> "Good Evening, $name"
        }
    }
}