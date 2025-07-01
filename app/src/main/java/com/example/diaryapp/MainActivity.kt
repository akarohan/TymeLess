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

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private var selectedDate: Long = System.currentTimeMillis()
    private lateinit var db: DiaryDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        db = DiaryDatabase.getDatabase(this)

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
    }

    private fun updateDrawerHeader() {
        val nameTextView = findViewById<TextView>(R.id.navUserName)
        val usernameTextView = findViewById<TextView>(R.id.navUserUsername)
        val prefs = EncryptedSharedPreferences.create(
            "diary_auth_prefs",
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        nameTextView?.text = prefs.getString("name", "User")
        usernameTextView?.text = prefs.getString("username", "username")
        nameTextView?.setTextColor(android.graphics.Color.WHITE)
        usernameTextView?.setTextColor(android.graphics.Color.WHITE)
        // Set header background to black
        (nameTextView?.parent as? View)?.setBackgroundColor(android.graphics.Color.BLACK)
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
}