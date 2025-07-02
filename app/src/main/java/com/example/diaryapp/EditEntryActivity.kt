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
import android.text.Html
import android.text.Spannable
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.text.style.StrikethroughSpan
import android.graphics.Typeface
import android.content.res.ColorStateList
import android.widget.LinearLayout

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
    private lateinit var mainEditText: EditText
    private var isBoldActive = false
    private var isItalicActive = false
    private var isUnderlineActive = false
    private var isStrikethroughActive = false

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
        mainEditText = findViewById(R.id.mainEditText)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            saveEntry()
        }

        val toolbarTitle = findViewById<TextView>(R.id.toolbarTitle)
        val datePickerChip = findViewById<Chip>(R.id.datePickerChip)

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
                val plainText = if (entry.htmlContent != null) Html.fromHtml(entry.htmlContent ?: "", Html.FROM_HTML_MODE_LEGACY) else ""
                mainEditText.setText(plainText)
                entryId = entry.id
                imageUris.clear()
                entry.imagePaths?.forEach { path ->
                    imageUris.add(android.net.Uri.parse(path))
                }
                imageBlockAdapter.notifyDataSetChanged()
            } else {
                titleEditText.setText("")
                mainEditText.setText("")
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

        val cardView = findViewById<androidx.cardview.widget.CardView>(R.id.cardView)
        cardView.setOnClickListener {
            mainEditText.requestFocus()
            // Force cursor to the start by resetting the HTML content
            val currentHtml = mainEditText.text.toString()
            mainEditText.setText("") // Clear first to reset cursor
            mainEditText.setText(currentHtml)
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(mainEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }

        setupFormatButtons()
    }

    override fun onPause() {
        super.onPause()
        saveEntry()
    }

    private fun saveEntry() {
        val htmlContent = Html.toHtml(mainEditText.text, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
        val imagePaths = imageUris.map { uri ->
            if (uri.scheme == "file") uri.path!! else uri.toString()
        }
        val title = titleEditText.text.toString()
        val entry = if (entryId != null) {
            DiaryEntry(
                id = entryId!!,
                date = entryDate,
                htmlContent = htmlContent,
                imagePaths = imagePaths,
                title = title
            )
        } else {
            DiaryEntry(
                date = entryDate,
                htmlContent = htmlContent,
                imagePaths = imagePaths,
                title = title
            )
        }
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

    private fun setupFormatButtons() {
        val btnBold = findViewById<MaterialButton>(R.id.btnBold)
        val btnItalic = findViewById<MaterialButton>(R.id.btnItalic)
        val btnUnderline = findViewById<MaterialButton>(R.id.btnUnderline)
        val btnStrikethrough = findViewById<MaterialButton>(R.id.btnStrikethrough)

        btnBold.setOnClickListener {
            val start = mainEditText.selectionStart
            val end = mainEditText.selectionEnd
            if (start == end) {
                isBoldActive = !isBoldActive
                updateButtonStyle(btnBold, isBoldActive)
                Toast.makeText(this, "Bold mode: ${if (isBoldActive) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Bold clicked: selection $start-$end", Toast.LENGTH_SHORT).show()
                toggleStyle(Typeface.BOLD, btnBold)
            }
        }
        btnItalic.setOnClickListener {
            val start = mainEditText.selectionStart
            val end = mainEditText.selectionEnd
            if (start == end) {
                isItalicActive = !isItalicActive
                updateButtonStyle(btnItalic, isItalicActive)
                Toast.makeText(this, "Italic mode: ${if (isItalicActive) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Italic clicked: selection $start-$end", Toast.LENGTH_SHORT).show()
                toggleStyle(Typeface.ITALIC, btnItalic)
            }
        }
        btnUnderline.setOnClickListener {
            val start = mainEditText.selectionStart
            val end = mainEditText.selectionEnd
            if (start == end) {
                isUnderlineActive = !isUnderlineActive
                updateButtonStyle(btnUnderline, isUnderlineActive)
                Toast.makeText(this, "Underline mode: ${if (isUnderlineActive) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Underline clicked: selection $start-$end", Toast.LENGTH_SHORT).show()
                toggleUnderline(btnUnderline)
            }
        }
        btnStrikethrough.setOnClickListener {
            val start = mainEditText.selectionStart
            val end = mainEditText.selectionEnd
            if (start == end) {
                isStrikethroughActive = !isStrikethroughActive
                updateButtonStyle(btnStrikethrough, isStrikethroughActive)
                Toast.makeText(this, "Strikethrough mode: ${if (isStrikethroughActive) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Strikethrough clicked: selection $start-$end", Toast.LENGTH_SHORT).show()
                toggleStrikethrough(btnStrikethrough)
            }
        }

        // Add TextWatcher for formatting mode
        mainEditText.addTextChangedListener(object : android.text.TextWatcher {
            private var lastStart = 0
            private var lastCount = 0
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                lastStart = start
                lastCount = after
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (lastCount > 0 && s != null) {
                    val end = lastStart + lastCount
                    if (isBoldActive) {
                        s.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), lastStart, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    if (isItalicActive) {
                        s.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.ITALIC), lastStart, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    if (isUnderlineActive) {
                        s.setSpan(android.text.style.UnderlineSpan(), lastStart, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    if (isStrikethroughActive) {
                        s.setSpan(android.text.style.StrikethroughSpan(), lastStart, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
            }
        })
    }

    private fun toggleStyle(style: Int, button: MaterialButton) {
        val start = mainEditText.selectionStart
        val end = mainEditText.selectionEnd
        if (start == end) return // No selection

        val spannable = mainEditText.text as Spannable
        val spans = spannable.getSpans(start, end, StyleSpan::class.java)
        var exists = false
        for (span in spans) {
            if (span.style == style) {
                spannable.removeSpan(span)
                exists = true
            }
        }
        if (!exists) {
            spannable.setSpan(StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        updateButtonStyle(button, !exists)
    }

    private fun toggleUnderline(button: MaterialButton) {
        val start = mainEditText.selectionStart
        val end = mainEditText.selectionEnd
        if (start == end) return

        val spannable = mainEditText.text as Spannable
        val spans = spannable.getSpans(start, end, UnderlineSpan::class.java)
        var exists = false
        for (span in spans) {
            spannable.removeSpan(span)
            exists = true
        }
        if (!exists) {
            spannable.setSpan(UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        updateButtonStyle(button, !exists)
    }

    private fun toggleStrikethrough(button: MaterialButton) {
        val start = mainEditText.selectionStart
        val end = mainEditText.selectionEnd
        if (start == end) return

        val spannable = mainEditText.text as Spannable
        val spans = spannable.getSpans(start, end, StrikethroughSpan::class.java)
        var exists = false
        for (span in spans) {
            spannable.removeSpan(span)
            exists = true
        }
        if (!exists) {
            spannable.setSpan(StrikethroughSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        updateButtonStyle(button, !exists)
    }

    private fun updateButtonStyle(button: MaterialButton, isActive: Boolean) {
        if (isActive) {
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.black))
            button.setTextColor(ContextCompat.getColor(this, R.color.white))
            button.iconTint = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
        } else {
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
            button.setTextColor(ContextCompat.getColor(this, R.color.black))
            button.iconTint = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.black))
        }
    }
} 