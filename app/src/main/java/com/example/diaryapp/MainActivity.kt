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
import jp.wasabeef.blurry.Blurry
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import android.graphics.Bitmap
import android.widget.ImageButton

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
    private lateinit var pickCoverImageLauncher: androidx.activity.result.ActivityResultLauncher<String>
    private lateinit var takeCoverImageLauncher: androidx.activity.result.ActivityResultLauncher<android.net.Uri>
    private var coverCameraImageUri: android.net.Uri? = null
    private val COVER_CAMERA_PERMISSION_REQUEST_CODE = 1002
    private lateinit var pickThemeHeaderPicLauncher: androidx.activity.result.ActivityResultLauncher<String>
    private lateinit var cropThemeHeaderPicLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
    private lateinit var prefs: SharedPreferences
    private lateinit var toolbarBackgroundImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        toolbarBackgroundImage = findViewById(R.id.toolbarBackgroundImage)

        prefs = getEncryptedPrefs()

        // Set theme pic or color on the app bar (header)
        val themeHeaderPicUri = prefs.getString("theme_header_pic_uri", null)
        if (themeHeaderPicUri != null && File(themeHeaderPicUri).exists()) {
            Glide.with(this)
                .load(Uri.fromFile(File(themeHeaderPicUri)))
                .centerCrop()
                .into(toolbarBackgroundImage)
            toolbarBackgroundImage.visibility = View.VISIBLE
            binding.appBarMain.toolbar.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        } else {
            toolbarBackgroundImage.visibility = View.GONE
            val themeColor = ThemeUtils.getCurrentThemeColor(this)
            binding.appBarMain.toolbar.setBackgroundColor(themeColor)
        }

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
                    // Clear checked state for all items
                    val menu = navView.menu
                    for (i in 0 until menu.size()) {
                        menu.getItem(i).isChecked = false
                    }
                    true
                }
                R.id.nav_settings -> {
                    drawerLayout.closeDrawers()
                    startActivity(Intent(this, SettingsActivity::class.java))
                    // Clear checked state for all items
                    val menu = navView.menu
                    for (i in 0 until menu.size()) {
                        menu.getItem(i).isChecked = false
                    }
                    true
                }
                R.id.nav_customize_login -> {
                    drawerLayout.closeDrawers()
                    startActivity(Intent(this, LoginPageCustomizeActivity::class.java))
                    // Clear checked state for all items
                    val menu = navView.menu
                    for (i in 0 until menu.size()) {
                        menu.getItem(i).isChecked = false
                    }
                    true
                }
                R.id.nav_logout -> {
                    drawerLayout.closeDrawers()
                    // Clear only session-related preferences, preserve login credentials and images
                    prefs.edit()
                        // .remove("profile_pic_uri")
                        // .remove("cover_pic_uri")
                        .apply()
                    // Redirect to AuthActivity
                    val intent = Intent(this, AuthActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_change_theme -> {
                    drawerLayout.closeDrawers()
                    val prefsColor = getSharedPreferences("theme_prefs", MODE_PRIVATE)
                    val defaultColor = ContextCompat.getColor(this, R.color.greyback)
                    val currentColor = prefsColor.getInt("theme_color", defaultColor)
                    
                    // Comprehensive list of colors with names
                    val colorOptions = intArrayOf(
                        0xFFF44336.toInt(), // Red
                        0xFFE91E63.toInt(), // Pink
                        0xFF9C27B0.toInt(), // Purple
                        0xFF673AB7.toInt(), // Deep Purple
                        0xFF3F51B5.toInt(), // Indigo
                        0xFF2196F3.toInt(), // Blue
                        0xFF03A9F4.toInt(), // Light Blue
                        0xFF00BCD4.toInt(), // Cyan
                        0xFF009688.toInt(), // Teal
                        0xFF4CAF50.toInt(), // Green
                        0xFF8BC34A.toInt(), // Light Green
                        0xFFCDDC39.toInt(), // Lime
                        0xFFFFEB3B.toInt(), // Yellow
                        0xFFFFC107.toInt(), // Amber
                        0xFFFF9800.toInt(), // Orange
                        0xFFFF5722.toInt(), // Deep Orange
                        0xFF795548.toInt(), // Brown
                        0xFF9E9E9E.toInt(), // Grey
                        0xFF607D8B.toInt(), // Blue Grey
                        0xFF000000.toInt(), // Black
                        0xFFFFFFFF.toInt(), // White
                        0xFFE0E0E0.toInt(), // Light Grey
                        0xFFF5F5F5.toInt(), // Very Light Grey
                        0xFFFAFAFA.toInt(), // Off White
                        0xFF424242.toInt(), // Dark Grey
                        0xFF212121.toInt(), // Very Dark Grey
                        0xFF1A237E.toInt(), // Dark Blue
                        0xFF0D47A1.toInt(), // Deep Blue
                        0xFF004D40.toInt(), // Dark Teal
                        0xFF1B5E20.toInt(), // Dark Green
                        0xFFBF360C.toInt(), // Dark Orange
                        0xFF4A148C.toInt(), // Dark Purple
                        0xFF880E4F.toInt()  // Dark Pink
                    )
                    
                    val colorNames = arrayOf(
                        "Red", "Pink", "Purple", "Deep Purple", "Indigo", "Blue", "Light Blue", 
                        "Cyan", "Teal", "Green", "Light Green", "Lime", "Yellow", "Amber", 
                        "Orange", "Deep Orange", "Brown", "Grey", "Blue Grey", "Black", 
                        "White", "Light Grey", "Very Light Grey", "Off White", "Dark Grey", 
                        "Very Dark Grey", "Dark Blue", "Deep Blue", "Dark Teal", "Dark Green", 
                        "Dark Orange", "Dark Purple", "Dark Pink"
                    )
                    
                    AlertDialog.Builder(this)
                        .setTitle("Pick a theme color")
                        .setItems(colorNames) { _, which ->
                            val color = colorOptions[which]
                            Log.d("THEME_COLOR", "Selected color: ${String.format("#%06X", 0xFFFFFF and color)}")
                            prefsColor.edit().putInt("theme_color", color).apply()
                            
                            // Remove theme header image if it exists
                            val themeHeaderPicFile = File(filesDir, "theme_header_pic.jpg")
                            if (themeHeaderPicFile.exists()) {
                                themeHeaderPicFile.delete()
                                Log.d("THEME_COLOR", "Deleted theme header image file")
                            }
                            prefs.edit().remove("theme_header_pic_uri").apply()
                            
                            // Hide the background image and show the color
                            if (::toolbarBackgroundImage.isInitialized) {
                                toolbarBackgroundImage.visibility = View.GONE
                                Log.d("THEME_COLOR", "Hidden toolbar background image")
                            }
                            binding.appBarMain.toolbar.setBackgroundColor(color)
                            Log.d("THEME_COLOR", "Applied color to toolbar")
                            
                            // Force refresh the toolbar
                            binding.appBarMain.toolbar.invalidate()
                            binding.appBarMain.toolbar.requestLayout()
                            
                            Toast.makeText(this, "Theme color applied!", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                    true
                }
                R.id.nav_change_theme_pic -> {
                    drawerLayout.closeDrawers()
                    pickThemeHeaderPicLauncher.launch("image/*")
                    true
                }
                R.id.nav_backup_restore -> {
                    drawerLayout.closeDrawers()
                    startActivity(Intent(this, BackupRestoreActivity::class.java))
                    true
                }

                else -> false
            }
        }

        val headerView = navView.getHeaderView(0)
        val profileImageView = headerView.findViewById<ImageView>(R.id.imageView)
        val profileCameraButton = headerView.findViewById<ImageButton>(R.id.profileCameraButton)
        val coverCameraButton = headerView.findViewById<ImageButton>(R.id.coverCameraButton)
        val nameTextView = headerView.findViewById<TextView>(R.id.navUserName)
        val usernameTextView = headerView.findViewById<TextView>(R.id.navUserUsername)
        val profilePicUri = prefs.getString("profile_pic_uri", null)
        val coverPicUri = prefs.getString("cover_pic_uri", null)
        Log.d("PROFILE_IMAGE", "updateDrawerHeader profilePicUri: $profilePicUri")
        var fileExists = false
        var file: File? = null
        if (profilePicUri != null) {
            file = if (profilePicUri.startsWith("/")) File(profilePicUri) else null
            fileExists = file?.exists() == true
            Log.d("PROFILE_IMAGE", "File exists: $fileExists")
        }
        if (profilePicUri != null && profileImageView != null && fileExists) {
            val uri = android.net.Uri.fromFile(file)
            // Set the profile image in the nav drawer header
            Glide.with(this)
                .load(uri)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(profileImageView)
        } else if (profileImageView != null) {
            profileImageView.setImageResource(R.drawable.ic_user_placeholder)
        }
        // Set the blurred background (cover photo)
        val blurredBackground = headerView.findViewById<ImageView>(R.id.blurredBackground)
        if (coverPicUri != null && blurredBackground != null) {
            val uri = if (coverPicUri.startsWith("/")) {
                android.net.Uri.fromFile(java.io.File(coverPicUri))
            } else {
                android.net.Uri.parse(coverPicUri)
            }
            Glide.with(this)
                .asBitmap()
                .load(uri)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                    override fun onResourceReady(resource: android.graphics.Bitmap, transition: com.bumptech.glide.request.transition.Transition<in android.graphics.Bitmap>?) {
                        val scaled = if (resource.width > 800 || resource.height > 800) {
                            android.graphics.Bitmap.createScaledBitmap(resource, 800, 800 * resource.height / resource.width, true)
                        } else resource
                        jp.wasabeef.blurry.Blurry.with(this@MainActivity)
                            .radius(20)
                            .from(scaled)
                            .into(blurredBackground)
                    }
                    override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
                })
        } else if (profilePicUri != null && blurredBackground != null) {
            // fallback: use profile pic as cover if no cover set
            val uri = if (profilePicUri.startsWith("/")) {
                android.net.Uri.fromFile(java.io.File(profilePicUri))
            } else {
                android.net.Uri.parse(profilePicUri)
            }
            Glide.with(this)
                .asBitmap()
                .load(uri)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                    override fun onResourceReady(resource: android.graphics.Bitmap, transition: com.bumptech.glide.request.transition.Transition<in android.graphics.Bitmap>?) {
                        val scaled = if (resource.width > 800 || resource.height > 800) {
                            android.graphics.Bitmap.createScaledBitmap(resource, 800, 800 * resource.height / resource.width, true)
                        } else resource
                        jp.wasabeef.blurry.Blurry.with(this@MainActivity)
                            .radius(20)
                            .from(scaled)
                            .into(blurredBackground)
                    }
                    override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
                })
        }

        profileCameraButton?.setOnClickListener {
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

        coverCameraButton?.setOnClickListener {
            val options = arrayOf("Choose a Picture", "Take a Picture", "Remove Picture")
            AlertDialog.Builder(this)
                .setTitle("Cover Photo")
                .setItems(options) { dialog, which ->
                    when (which) {
                        0 -> pickCoverImageLauncher.launch("image/*")
                        1 -> {
                            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), COVER_CAMERA_PERMISSION_REQUEST_CODE)
                                return@setItems
                            }
                            launchCameraForCoverImage()
                        }
                        2 -> removeCoverImage()
                    }
                }
                .show()
        }

        // Set greeting message below the logo based on time and user name
        val greetingTextView = binding.appBarMain.toolbar.findViewById<TextView>(R.id.greeting)
        val userName = prefs.getString("name", "User") ?: "User"
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
        pickCoverImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                saveCoverImageFromUri(it)
            }
        }
        takeCoverImageLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            val file = File(filesDir, "cover_image.jpg")
            if (success && file.exists() && file.length() > 0) {
                saveCoverImageFromUri(android.net.Uri.fromFile(file))
            } else {
                Toast.makeText(this, "Failed to save cover photo. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }

        pickThemeHeaderPicLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val destUri = Uri.fromFile(File(filesDir, "theme_header_pic_cropped.jpg"))
                val uCrop = UCrop.of(it, destUri)
                    .withAspectRatio(16f, 9f)
                    .withMaxResultSize(1200, 675)
                cropThemeHeaderPicLauncher.launch(uCrop.getIntent(this))
            }
        }
        cropThemeHeaderPicLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val resultUri = UCrop.getOutput(data!!)
                if (resultUri != null) {
                    saveThemeHeaderPicFromUri(resultUri)
                }
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
        val blurredBackground = headerView.findViewById<ImageView>(R.id.blurredBackground)
        val profileImageView = headerView.findViewById<ImageView>(R.id.imageView)
        val nameTextView = headerView.findViewById<TextView>(R.id.navUserName)
        val usernameTextView = headerView.findViewById<TextView>(R.id.navUserUsername)
        val prefs = getEncryptedPrefs()
        val coverPicUri = prefs.getString("cover_pic_uri", null)
        val profilePicUri = prefs.getString("profile_pic_uri", null)
        
        // Update username and name in the navigation drawer header
        val userName = prefs.getString("name", "User") ?: "User"
        val userUsername = prefs.getString("username", "user") ?: "user"
        nameTextView?.text = userName
        usernameTextView?.text = userUsername
        
        // Update profile image with cache busting
        var fileExists = false
        var file: File? = null
        if (profilePicUri != null) {
            file = if (profilePicUri.startsWith("/")) File(profilePicUri) else null
            fileExists = file?.exists() == true
        }
        if (profilePicUri != null && profileImageView != null && fileExists) {
            val uri = android.net.Uri.fromFile(file)
            Glide.with(this)
                .load(uri)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(profileImageView)
        } else if (profileImageView != null) {
            profileImageView.setImageResource(R.drawable.ic_user_placeholder)
        }
        // Update blurred background (cover photo) with cache busting
        if (coverPicUri != null && blurredBackground != null && File(coverPicUri).exists()) {
            val uri = if (coverPicUri.startsWith("/")) {
                android.net.Uri.fromFile(java.io.File(coverPicUri))
            } else {
                android.net.Uri.parse(coverPicUri)
            }
            Glide.with(this)
                .asBitmap()
                .load(uri)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(object : com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?) {
                        val scaled = if (resource.width > 800 || resource.height > 800) {
                            Bitmap.createScaledBitmap(resource, 800, 800 * resource.height / resource.width, true)
                        } else resource
                        jp.wasabeef.blurry.Blurry.with(this@MainActivity)
                            .radius(20)
                            .from(scaled)
                            .into(blurredBackground)
                    }
                    override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
                })
        } else if (profileImageView != null && blurredBackground != null) {
            // fallback: use profile pic as cover if no cover set
            val uri = if (profilePicUri != null && profilePicUri.startsWith("/")) {
                android.net.Uri.fromFile(java.io.File(profilePicUri))
            } else if (profilePicUri != null) {
                android.net.Uri.parse(profilePicUri)
            } else {
                null
            }
            if (uri != null) {
            Glide.with(this)
                .asBitmap()
                .load(uri)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                    .into(object : com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?) {
                        val scaled = if (resource.width > 800 || resource.height > 800) {
                            Bitmap.createScaledBitmap(resource, 800, 800 * resource.height / resource.width, true)
                        } else resource
                            jp.wasabeef.blurry.Blurry.with(this@MainActivity)
                            .radius(20)
                            .from(scaled)
                            .into(blurredBackground)
                    }
                    override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
                })
            }
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
        if (::toolbarBackgroundImage.isInitialized) {
            val themeHeaderPicUri = prefs.getString("theme_header_pic_uri", null)
            if (themeHeaderPicUri != null && File(themeHeaderPicUri).exists()) {
                Glide.with(this)
                    .load(Uri.fromFile(File(themeHeaderPicUri)))
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(toolbarBackgroundImage)
                toolbarBackgroundImage.visibility = View.VISIBLE
                binding.appBarMain.toolbar.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                Log.d("THEME_PIC", "onResume: Loaded theme header image: $themeHeaderPicUri")
            } else {
                toolbarBackgroundImage.visibility = View.GONE
                val themeColor = ThemeUtils.getCurrentThemeColor(this)
                binding.appBarMain.toolbar.setBackgroundColor(themeColor)
                Log.d("THEME_PIC", "onResume: No theme image, using color: ${String.format("#%06X", 0xFFFFFF and themeColor)}")
            }
        }
        updateDrawerHeader()
        // Update toolbar profile image on resume
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
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .placeholder(R.drawable.ic_user_placeholder)
                .error(R.drawable.ic_user_placeholder)
                .into(toolbarProfileImageView)
        } else if (toolbarProfileImageView != null) {
            toolbarProfileImageView.setImageResource(R.drawable.ic_user_placeholder)
        }
        // Update greeting message on resume
        val greetingTextView = binding.appBarMain.toolbar.findViewById<TextView>(R.id.greeting)
        val userName = prefs.getString("name", "User") ?: "User"
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

    private fun launchCameraForCoverImage() {
        val file = File(filesDir, "cover_image.jpg")
        coverCameraImageUri = androidx.core.content.FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            file
        )
        takeCoverImageLauncher.launch(coverCameraImageUri)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchCameraForProfileImage()
        } else if (requestCode == COVER_CAMERA_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchCameraForCoverImage()
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
            Log.d("PROFILE_IMAGE", "Saved profile image to: ${file.absolutePath}, exists: ${file.exists()}")
            // Save file path in preferences
            prefs.edit().putString("profile_pic_uri", file.absolutePath).apply()
            Log.d("PROFILE_IMAGE", "Saved profile_pic_uri in prefs: ${file.absolutePath}")
            updateDrawerHeader()

            // Update toolbar/home header profile image immediately
            val toolbarProfileImageView = binding.appBarMain.toolbar.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.profileImageView)
            val profilePicUri = prefs.getString("profile_pic_uri", null)
            Log.d("PROFILE_IMAGE", "Toolbar profilePicUri: $profilePicUri")
            if (profilePicUri != null && toolbarProfileImageView != null) {
                val uri = if (profilePicUri.startsWith("/")) {
                    android.net.Uri.fromFile(java.io.File(profilePicUri))
                } else {
                    android.net.Uri.parse(profilePicUri)
                }
                Glide.with(this)
                    .load(uri)
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

    private fun saveCoverImageFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(filesDir, "cover_image.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            // Save file path in preferences
            prefs.edit().putString("cover_pic_uri", file.absolutePath).apply()
            updateDrawerHeader()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeProfileImage() {
        val file = File(filesDir, "profile_image.jpg")
        if (file.exists()) file.delete()
        prefs.edit().remove("profile_pic_uri").apply()
        updateDrawerHeader()
        // Update toolbar/home header profile image immediately
        val toolbarProfileImageView = binding.appBarMain.toolbar.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.profileImageView)
        toolbarProfileImageView?.setImageResource(R.drawable.ic_user_placeholder)
    }

    private fun removeCoverImage() {
        val file = File(filesDir, "cover_image.jpg")
        if (file.exists()) file.delete()
        prefs.edit().remove("cover_pic_uri").apply()
        updateDrawerHeader()
    }

    private fun saveThemeHeaderPicFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val timestamp = System.currentTimeMillis()
            val file = File(filesDir, "theme_header_pic_$timestamp.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            prefs.edit().putString("theme_header_pic_uri", file.absolutePath).apply()
            
            // Immediately apply the theme header image to the main toolbar
            if (::toolbarBackgroundImage.isInitialized) {
                Glide.with(this)
                    .load(Uri.fromFile(file))
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(toolbarBackgroundImage)
                toolbarBackgroundImage.visibility = View.VISIBLE
                binding.appBarMain.toolbar.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                Log.d("THEME_PIC", "Applied theme header image immediately: ${file.absolutePath}")
            }
            
            updateDrawerHeader()
            Toast.makeText(this, "Theme header image applied!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to apply theme image", Toast.LENGTH_SHORT).show()
        }
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