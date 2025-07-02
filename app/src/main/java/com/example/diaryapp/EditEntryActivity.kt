package com.example.diaryapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import jp.wasabeef.richeditor.RichEditor
import kotlinx.coroutines.*
import androidx.lifecycle.lifecycleScope
import android.widget.EditText
import android.widget.Button
import androidx.appcompat.widget.Toolbar
import android.widget.TextView
import android.widget.NumberPicker
import android.net.Uri
import android.provider.MediaStore
import android.content.ContentValues
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.chip.Chip
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class EditEntryActivity : AppCompatActivity() {
    private lateinit var richEditor: RichEditor
    private lateinit var db: DiaryDatabase
    private var entryDate: Long = 0L
    private lateinit var titleEditText: EditText
    private lateinit var saveButton: Button
    private var entryId: Int? = null
    private var isNewEntry: Boolean = true
    private lateinit var btnGallery: ImageButton
    private lateinit var btnCamera: ImageButton
    private var imageUri: Uri? = null
    private lateinit var imagesRecyclerView: RecyclerView
    private lateinit var imageBlockAdapter: ImageBlockAdapter
    private val imageUris = mutableListOf<Uri>()

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val filePath = copyUriToInternalStorage(it)
            if (filePath != null) {
                imageUris.add(Uri.fromFile(java.io.File(filePath)))
                imageBlockAdapter.notifyItemInserted(imageUris.size - 1)
            } else {
                Toast.makeText(this, "Failed to copy image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success && imageUri != null) {
            imageUris.add(imageUri!!)
            imageBlockAdapter.notifyItemInserted(imageUris.size - 1)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_entry)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            saveEntry()
            finish()
        }

        val toolbarTitle = findViewById<TextView>(R.id.toolbarTitle)
        val datePickerChip = findViewById<Chip>(R.id.datePickerChip)

        richEditor = findViewById(R.id.richEditor)
        val editorScrollView = findViewById<android.widget.ScrollView>(R.id.editorScrollView)
        titleEditText = findViewById(R.id.titleEditText)
        saveButton = findViewById(R.id.saveButton)
        db = DiaryDatabase.getDatabase(this)

        entryDate = intent.getLongExtra("entry_date", System.currentTimeMillis())
        entryId = null

        // Load existing entries for this date (if any)
        lifecycleScope.launch {
            val entries = withContext(Dispatchers.IO) {
                db.diaryEntryDao().getEntriesByDate(entryDate)
            }
            if (entries.isNotEmpty()) {
                // Load the first entry for editing, or customize as needed
                val entry = entries[0]
                titleEditText.setText(entry.title ?: "")
                richEditor.html = entry.htmlContent ?: ""
                entryId = entry.id
                imageUris.clear()
                entry.imagePaths?.forEach { path ->
                    imageUris.add(android.net.Uri.parse(path))
                }
                imageBlockAdapter.notifyDataSetChanged()
            } else {
                titleEditText.setText("")
                richEditor.html = ""
                entryId = null
                imageUris.clear()
                imageBlockAdapter.notifyDataSetChanged()
            }
        }

        // Format and set initial date on button
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = entryDate
        updateDateChipText(datePickerChip, calendar)

        datePickerChip.setOnClickListener {
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH)
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
            val datePickerDialog = android.app.DatePickerDialog(
                this,
                { _, y, m, d ->
                    calendar.set(y, m, d)
                    entryDate = calendar.timeInMillis
                    updateDateChipText(datePickerChip, calendar)
                }, year, month, day)
            datePickerDialog.show()
        }

        saveButton.setOnClickListener {
            saveEntry()
        }

        btnGallery = findViewById(R.id.btnGallery)
        btnCamera = findViewById(R.id.btnCamera)

        imagesRecyclerView = findViewById(R.id.imagesRecyclerView)
        imageBlockAdapter = ImageBlockAdapter(imageUris) { position ->
            imageUris.removeAt(position)
            imageBlockAdapter.notifyItemRemoved(position)
        }
        imagesRecyclerView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        imagesRecyclerView.adapter = imageBlockAdapter

        btnGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        btnCamera.setOnClickListener {
            val photoUri = createImageUri()
            imageUri = photoUri
            cameraLauncher.launch(photoUri)
        }

        // Formatting buttons
        val btnBold = findViewById<MaterialButton>(R.id.btnBold)
        val btnItalic = findViewById<MaterialButton>(R.id.btnItalic)
        val btnUnderline = findViewById<MaterialButton>(R.id.btnUnderline)
        val btnStrikethrough = findViewById<MaterialButton>(R.id.btnStrikethrough)

        fun updateFormatButtonColors(button: MaterialButton) {
            val context = button.context
            if (button.isChecked) {
                button.backgroundTintList = ContextCompat.getColorStateList(context, R.color.black)
                button.setTextColor(ContextCompat.getColor(context, R.color.white))
            } else {
                button.backgroundTintList = ContextCompat.getColorStateList(context, R.color.white)
                button.setTextColor(ContextCompat.getColor(context, R.color.black))
            }
        }

        btnBold.setOnClickListener {
            btnBold.isChecked = !btnBold.isChecked
            updateFormatButtonColors(btnBold)
            richEditor.setBold()
        }
        btnItalic.setOnClickListener {
            btnItalic.isChecked = !btnItalic.isChecked
            updateFormatButtonColors(btnItalic)
            richEditor.setItalic()
        }
        btnUnderline.setOnClickListener {
            btnUnderline.isChecked = !btnUnderline.isChecked
            updateFormatButtonColors(btnUnderline)
            richEditor.setUnderline()
        }
        btnStrikethrough.setOnClickListener {
            btnStrikethrough.isChecked = !btnStrikethrough.isChecked
            updateFormatButtonColors(btnStrikethrough)
            richEditor.setStrikeThrough()
        }
        listOf(btnBold, btnItalic, btnUnderline, btnStrikethrough).forEach { updateFormatButtonColors(it) }

        richEditor.setOnTextChangeListener {
            editorScrollView.post {
                editorScrollView.fullScroll(android.view.View.FOCUS_DOWN)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        saveEntry()
    }

    private fun saveEntry() {
        val htmlContent = richEditor.html ?: ""
        val imagePaths = imageUris.map { uri ->
            if (uri.scheme == "file") uri.path!! else uri.toString()
        } // Save as list of Strings
        val title = titleEditText.text.toString()
        val entry = DiaryEntry(
            id = entryId ?: 0,
            date = entryDate,
            htmlContent = htmlContent,
            imagePaths = imagePaths,
            title = title
        )
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db.diaryEntryDao().insertOrUpdate(entry)
            }
            android.widget.Toast.makeText(this@EditEntryActivity, "Entry saved", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateDateChipText(chip: Chip, calendar: java.util.Calendar) {
        val format = java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale.getDefault())
        chip.text = format.format(calendar.time)
    }

    private fun createImageUri(): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "new_image_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    private fun copyUriToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = java.io.File(filesDir, "image_${System.currentTimeMillis()}.jpg")
            val outputStream = java.io.FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
} 