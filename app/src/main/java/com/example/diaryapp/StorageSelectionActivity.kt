package com.example.diaryapp

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.json.JSONArray
import java.util.UUID

class StorageSelectionActivity : AppCompatActivity() {
    private val PICK_PATH_REQUEST_CODE = 2001
    private lateinit var prefs: SharedPreferences
    private lateinit var radioGroup: RadioGroup
    private lateinit var appStorageRadio: RadioButton
    private lateinit var localStorageRadio: RadioButton
    private lateinit var btnContinue: Button
    private lateinit var tvRestorePrompt: TextView
    private var selectedPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_storage_selection)
        prefs = getEncryptedPrefs()

        radioGroup = findViewById(R.id.storageRadioGroup)
        appStorageRadio = findViewById(R.id.radioAppStorage)
        localStorageRadio = findViewById(R.id.radioLocalStorage)
        btnContinue = findViewById(R.id.btnContinue)
        tvRestorePrompt = findViewById(R.id.tvRestorePrompt)
        tvRestorePrompt.visibility = View.GONE

        btnContinue.setOnClickListener {
            if (appStorageRadio.isChecked) {
                prefs.edit().putString("storage_type", "app").apply()
                ensureUserKey()
                goToMain()
            } else if (localStorageRadio.isChecked) {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                startActivityForResult(intent, PICK_PATH_REQUEST_CODE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_PATH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val uri: Uri? = data?.data
            if (uri != null) {
                selectedPath = uri.toString()
                prefs.edit().putString("storage_type", "local").putString("local_storage_path", selectedPath).apply()
                ensureUserKey()
                // Check for backup
                val backupFileName = "diary_backup.json"
                val childrenUri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(uri, android.provider.DocumentsContract.getTreeDocumentId(uri))
                val cursor = contentResolver.query(childrenUri, arrayOf(android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID, android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
                var backupUri: Uri? = null
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        val docId = cursor.getString(0)
                        val name = cursor.getString(1)
                        if (name == backupFileName) {
                            backupUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(uri, docId)
                            break
                        }
                    }
                    cursor.close()
                }
                if (backupUri != null) {
                    val inputStream = contentResolver.openInputStream(backupUri)
                    val json = inputStream?.bufferedReader().use { it?.readText() }
                    if (!json.isNullOrEmpty()) {
                        val jsonArray = JSONArray(json)
                        val backupUserKey = if (jsonArray.length() > 0) jsonArray.getJSONObject(0).optString("userKey", null) else null
                        val userKey = prefs.getString("user_key", null)
                        if (backupUserKey != null && userKey != null && backupUserKey == userKey) {
                            // Launch restore page
                            val intent = Intent(this, RestoreDataActivity::class.java)
                            intent.putExtra("backup_uri", backupUri.toString())
                            intent.putExtra("backup_path", backupUri.toString())
                            startActivity(intent)
                            finish()
                            return
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

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun ensureUserKey() {
        if (prefs.getString("user_key", null) == null) {
            val key = UUID.randomUUID().toString()
            prefs.edit().putString("user_key", key).apply()
        }
    }

    private fun getEncryptedPrefs() = EncryptedSharedPreferences.create(
        "diary_auth_prefs",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        this,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
} 