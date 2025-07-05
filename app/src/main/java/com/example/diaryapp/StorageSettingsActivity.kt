package com.example.diaryapp

import android.content.Context
import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.view.View
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.app.Activity
import android.content.SharedPreferences

class StorageSettingsActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private val PICK_PATH_REQUEST_CODE = 2001
    private lateinit var radioGroup: RadioGroup
    private lateinit var appStorageRadio: RadioButton
    private lateinit var localStorageRadio: RadioButton
    private lateinit var btnSyncNow: Button
    private lateinit var autoSyncLayout: View
    private lateinit var switchAutoSync: Switch
    private lateinit var pathLayout: View
    private lateinit var btnChoosePath: Button
    private lateinit var tvCurrentPath: TextView
    private lateinit var tvRestorePrompt: TextView
    private var selectedPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_storage_settings)
        
        val themeColor = ThemeUtils.getCurrentThemeColor(this)
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.setBackgroundColor(themeColor)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_circle_white)
        toolbar.setTitleTextColor(android.graphics.Color.BLACK)
        toolbar.setNavigationOnClickListener { finish() }
        
        prefs = getEncryptedPrefs()

        tvRestorePrompt = findViewById(R.id.tvRestorePrompt)
        tvRestorePrompt.visibility = View.GONE

        radioGroup = findViewById(R.id.storageRadioGroup)
        appStorageRadio = findViewById(R.id.radioAppStorage)
        localStorageRadio = findViewById(R.id.radioLocalStorage)
        btnSyncNow = findViewById(R.id.btnSyncNow)
        autoSyncLayout = findViewById(R.id.autoSyncLayout)
        switchAutoSync = findViewById(R.id.switchAutoSync)
        pathLayout = findViewById(R.id.pathLayout)
        btnChoosePath = findViewById(R.id.btnChoosePath)
        tvCurrentPath = findViewById(R.id.tvCurrentPath)

        val selected = prefs.getString("storage_type", "app")
        val autoSync = prefs.getBoolean("auto_sync", false)
        val currentPath = prefs.getString("local_storage_path", "") ?: ""
        switchAutoSync.isChecked = autoSync
        tvCurrentPath.text = currentPath

        fun updateVisibility() {
            if (localStorageRadio.isChecked) {
                btnSyncNow.visibility = View.VISIBLE
                autoSyncLayout.visibility = View.VISIBLE
                pathLayout.visibility = View.VISIBLE
            } else {
                btnSyncNow.visibility = View.GONE
                autoSyncLayout.visibility = View.GONE
                pathLayout.visibility = View.GONE
            }
        }

        updateVisibility()

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val editor = prefs.edit()
            if (checkedId == R.id.radioAppStorage) {
                editor.putString("storage_type", "app")
            } else {
                editor.putString("storage_type", "local")
            }
            editor.apply()
            updateVisibility()
        }

        switchAutoSync.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("auto_sync", isChecked).apply()
        }

        btnChoosePath.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, PICK_PATH_REQUEST_CODE)
        }

        btnSyncNow.setOnClickListener {
            val localPath = prefs.getString("local_storage_path", null)
            if (localPath == null) {
                runOnUiThread {
                    android.widget.Toast.makeText(this, "Please choose a local storage folder.", android.widget.Toast.LENGTH_LONG).show()
                }
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                startActivityForResult(intent, PICK_PATH_REQUEST_CODE)
                return@setOnClickListener
            }
            val folderUri = Uri.parse(localPath)
            val persistedUris = contentResolver.persistedUriPermissions
            val hasPermission = persistedUris.any { it.uri == folderUri && it.isWritePermission }
            if (!hasPermission) {
                runOnUiThread {
                    android.widget.Toast.makeText(this, "Storage permission lost. Please choose the folder again.", android.widget.Toast.LENGTH_LONG).show()
                }
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                startActivityForResult(intent, PICK_PATH_REQUEST_CODE)
                return@setOnClickListener
            }
            val backupFileName = "diary_backup.json"
            val contentResolver = applicationContext.contentResolver
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderUri, DocumentsContract.getTreeDocumentId(folderUri))
                    val cursor = contentResolver.query(childrenUri, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
                    var oldBackupDocId: String? = null
                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            val docId = cursor.getString(0)
                            val name = cursor.getString(1)
                            if (name == backupFileName) {
                                oldBackupDocId = docId
                                break
                            }
                        }
                        cursor.close()
                    }
                    if (oldBackupDocId != null) {
                        val oldBackupUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, oldBackupDocId)
                        DocumentsContract.deleteDocument(contentResolver, oldBackupUri)
                    }
                    val backupUri = DocumentsContract.createDocument(
                        contentResolver,
                        folderUri,
                        "application/json",
                        backupFileName
                    )
                    if (backupUri != null) {
                        val db = DiaryDatabase.getDatabase(this@StorageSettingsActivity)
                        val notes = db.noteDao().getAllNotes().value ?: emptyList()
                        val userKey = prefs.getString("user_key", "") ?: ""
                        val jsonArray = JSONArray()
                        for (note in notes) {
                            val obj = JSONObject()
                            obj.put("id", note.id)
                            obj.put("title", note.title)
                            obj.put("content", note.content)
                            obj.put("imagePaths", JSONArray(note.imagePaths))
                            obj.put("audioList", JSONArray())
                            obj.put("noteType", note.noteType)
                            obj.put("userKey", userKey)
                            jsonArray.put(obj)
                        }
                        val outputStream = contentResolver.openOutputStream(backupUri)
                        outputStream?.use { out: java.io.OutputStream ->
                            out.write(jsonArray.toString().toByteArray())
                        }
                        runOnUiThread {
                            android.widget.Toast.makeText(this@StorageSettingsActivity, "Backup created!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        runOnUiThread {
                            android.widget.Toast.makeText(this@StorageSettingsActivity, "Failed to create backup file.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        android.widget.Toast.makeText(this@StorageSettingsActivity, "Backup failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun ensureUserKey() {
        if (prefs.getString("user_key", null) == null) {
            val key = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("user_key", key).apply()
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_PATH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val uri: Uri? = data?.data
            if (uri != null) {
                val flags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                contentResolver.takePersistableUriPermission(uri, flags)
                prefs.edit().putString("local_storage_path", uri.toString()).apply()
                tvCurrentPath.text = uri.toString()
                ensureUserKey()
                val backupFileName = "diary_backup.json"
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
                val cursor = contentResolver.query(childrenUri, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
                var backupUri: Uri? = null
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        val docId = cursor.getString(0)
                        val name = cursor.getString(1)
                        if (name == backupFileName) {
                            backupUri = DocumentsContract.buildDocumentUriUsingTree(uri, docId)
                            break
                        }
                    }
                    cursor.close()
                }
                if (backupUri != null) {
                    val inputStream = contentResolver.openInputStream(backupUri)
                    val json = inputStream?.bufferedReader().use { reader: java.io.BufferedReader? -> reader?.readText() }
                    if (!json.isNullOrEmpty()) {
                        val jsonArray = JSONArray(json)
                        val backupUserKey = if (jsonArray.length() > 0) jsonArray.getJSONObject(0).optString("userKey", null) else null
                        val userKey = prefs.getString("user_key", null)
                        if (backupUserKey != null && userKey != null && backupUserKey == userKey) {
                            tvRestorePrompt.text = "Restore previous data from this location? (Will happen automatically on first launch)"
                            tvRestorePrompt.visibility = View.VISIBLE
                        } else {
                            tvRestorePrompt.text = "Backup found, but it does not match your account."
                            tvRestorePrompt.visibility = View.VISIBLE
                        }
                    }
                }
                goToMain()
            }
        }
    }

    private fun getEncryptedPrefs() = EncryptedSharedPreferences.create(
        "diary_storage_prefs",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        this,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
} 