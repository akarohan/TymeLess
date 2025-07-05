package com.example.diaryapp.ui.home

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Html
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.diaryapp.*
import com.example.diaryapp.data.Note
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import android.util.Log
import com.example.diaryapp.R

class EditNoteActivity : AppCompatActivity() {
    private lateinit var titleEditText: EditText
    private lateinit var mainEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var btnGallery: ImageButton
    private lateinit var btnCamera: ImageButton
    private lateinit var btnMic: ImageButton
    private lateinit var imagesRecyclerView: RecyclerView
    private lateinit var imageBlockAdapter: ImageBlockAdapter
    private lateinit var audioRecyclerView: RecyclerView
    private lateinit var audioChipAdapter: AudioChipAdapter
    private lateinit var toolbar: Toolbar
    private lateinit var toolbarTitle: TextView
    private lateinit var datePickerChip: Chip
    private lateinit var viewModel: NotesHomeViewModel
    private var imageUris = mutableListOf<Uri>()
    private var audioItems = mutableListOf<AudioItem>()
    private var audioRecorder: MediaRecorder? = null
    private var isRecording = false
    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlayingIndex: Int? = null
    private var imageUri: Uri? = null
    private var noteDate: Long = System.currentTimeMillis()
    private val REQUEST_RECORD_AUDIO_PERMISSION = 2001
    private val REQUEST_CAMERA_PERMISSION = 2002
    private var isBoldActive = false
    private var isItalicActive = false
    private var isUnderlineActive = false
    private var isStrikethroughActive = false
    private var noteId: Int = 0
    private var hasSaved = false
    private var noteType: String = "N"

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val filePath = copyUriToInternalStorage(it)
            if (filePath != null) {
                imageUris.add(Uri.fromFile(File(filePath)))
                imageBlockAdapter.notifyItemInserted(imageUris.size - 1)
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
        setContentView(R.layout.activity_edit_note)
        
        // Set theme color only on the app bar (header)
        val themeColor = ThemeUtils.getCurrentThemeColor(this)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setBackgroundColor(themeColor)

        setSupportActionBar(toolbar)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_circle_white)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        toolbarTitle = findViewById(R.id.toolbarTitle)
        datePickerChip = findViewById(R.id.datePickerChip)
        titleEditText = findViewById(R.id.titleEditText)
        mainEditText = findViewById(R.id.mainEditText)
        saveButton = findViewById(R.id.saveNoteButton)
        btnGallery = findViewById(R.id.btnGallery)
        btnCamera = findViewById(R.id.btnCamera)
        btnMic = findViewById(R.id.btnMic)
        imagesRecyclerView = findViewById(R.id.imagesRecyclerView)
        audioRecyclerView = findViewById(R.id.audioRecyclerView)
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)).get(NotesHomeViewModel::class.java)

        noteId = intent.getIntExtra("note_id", 0)
        noteType = intent.getStringExtra("note_type") ?: "N"
        if (noteId != 0) {
            // Editing existing note, prefill fields
            titleEditText.setText(intent.getStringExtra("note_title") ?: "")
            mainEditText.setText(intent.getStringExtra("note_content") ?: "")
            // TODO: Load images/audio if needed
        }

        // Date chip logic
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = noteDate
        updateDateChipText(datePickerChip, calendar)
        datePickerChip.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val datePickerDialog = DatePickerDialog(
                this,
                { _, y, m, d ->
                    calendar.set(y, m, d)
                    noteDate = calendar.timeInMillis
                    updateDateChipText(datePickerChip, calendar)
                }, year, month, day)
            datePickerDialog.show()
        }

        // Formatting bar
        setupFormatButtons()

        // Image logic
        imageBlockAdapter = ImageBlockAdapter(imageUris) { position ->
            imageUris.removeAt(position)
            imageBlockAdapter.notifyItemRemoved(position)
        }
        imagesRecyclerView.layoutManager = GridLayoutManager(this, 2)
        imagesRecyclerView.adapter = imageBlockAdapter
        btnGallery.setOnClickListener { galleryLauncher.launch("image/*") }
        btnCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            } else {
                val photoUri = createImageUri()
                if (photoUri != null) {
                    imageUri = photoUri
                    cameraLauncher.launch(photoUri)
                }
            }
        }

        // Audio logic
        audioChipAdapter = AudioChipAdapter(audioItems,
            onPlayPause = { item, pos -> playPauseAudio(item, pos) },
            onDelete = { item, pos ->
                audioItems.removeAt(pos)
                audioChipAdapter.notifyItemRemoved(pos)
            })
        audioRecyclerView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        audioRecyclerView.adapter = audioChipAdapter
        btnMic.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
            } else {
                if (isRecording) {
                    stopRecording()
                } else {
                    startRecording()
                }
            }
        }

        saveButton.setOnClickListener {
            autoSaveNote()
            Log.d("EditNoteActivity", "Save button pressed. Note: title='" + titleEditText.text + "', content='" + mainEditText.text + "', images=" + imageUris.size + ", audio=" + audioItems.size)
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        autoSaveNote()
    }

    private fun autoSaveNote() {
        if (hasSaved) return
        hasSaved = true
        val title = titleEditText.text.toString().trim()
        val content = mainEditText.text.toString().trim()
        Log.d("EditNoteActivity", "Auto-saving note: title='" + title + "', content='" + content + "', images=" + imageUris.size + ", audio=" + audioItems.size)
        if (title.isNotEmpty() || content.isNotEmpty() || imageUris.isNotEmpty() || audioItems.isNotEmpty()) {
            val note = Note(
                id = noteId,
                title = title,
                content = content,
                imagePaths = imageUris.map { it.toString() },
                audioList = audioItems,
                noteType = noteType
            )
            if (noteId != 0) {
                viewModel.update(note)
            } else {
                viewModel.insert(note)
            }
            Log.d("EditNoteActivity", "Note inserted/updated: $note")
        }
    }

    private fun updateDateChipText(chip: Chip, calendar: Calendar) {
        val dateFormat = java.text.SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
        chip.text = dateFormat.format(calendar.time)
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
            } else {
                toggleStyle(Typeface.BOLD, btnBold)
            }
        }
        btnItalic.setOnClickListener {
            val start = mainEditText.selectionStart
            val end = mainEditText.selectionEnd
            if (start == end) {
                isItalicActive = !isItalicActive
                updateButtonStyle(btnItalic, isItalicActive)
            } else {
                toggleStyle(Typeface.ITALIC, btnItalic)
            }
        }
        btnUnderline.setOnClickListener {
            val start = mainEditText.selectionStart
            val end = mainEditText.selectionEnd
            if (start == end) {
                isUnderlineActive = !isUnderlineActive
                updateButtonStyle(btnUnderline, isUnderlineActive)
            } else {
                toggleUnderline(btnUnderline)
            }
        }
        btnStrikethrough.setOnClickListener {
            val start = mainEditText.selectionStart
            val end = mainEditText.selectionEnd
            if (start == end) {
                isStrikethroughActive = !isStrikethroughActive
                updateButtonStyle(btnStrikethrough, isStrikethroughActive)
            } else {
                toggleStrikethrough(btnStrikethrough)
            }
        }

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
        if (start == end) return
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
            button.iconTint = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
        } else {
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
            button.setTextColor(ContextCompat.getColor(this, R.color.black))
            button.iconTint = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.black))
        }
    }

    private fun createImageUri(): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "note_image_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    private fun copyUriToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val file = File(filesDir, "note_image_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun setMicButtonRecordingState(isRecording: Boolean) {
        if (isRecording) {
            btnMic.background.setTint(ContextCompat.getColor(this, R.color.black))
            btnMic.setImageResource(R.drawable.ic_stop_circle_red)
            btnMic.clearColorFilter() // No filter, icon is already colored
        } else {
            btnMic.background.setTint(ContextCompat.getColor(this, R.color.white))
            btnMic.setImageResource(R.drawable.ic_mic_filled)
            btnMic.setColorFilter(ContextCompat.getColor(this, R.color.black))
        }
    }

    private fun startRecording() {
        val audioFile = File(filesDir, "audio_${System.currentTimeMillis()}.3gp")
        audioRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(audioFile.absolutePath)
            try {
                prepare()
                start()
                isRecording = true
                setMicButtonRecordingState(true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopRecording() {
        try {
            audioRecorder?.apply {
                stop()
                release()
            }
            audioRecorder = null
            isRecording = false
            setMicButtonRecordingState(false)
            // Save the audio file path
            val audioFile = File(filesDir, getLastAudioFileName())
            addAudioItem(audioFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getLastAudioFileName(): String {
        val files = filesDir.listFiles { file -> file.name.startsWith("audio_") && file.name.endsWith(".3gp") }
        return files?.maxByOrNull { it.lastModified() }?.name ?: ""
    }

    private fun addAudioItem(filePath: String) {
        val duration = getAudioDurationFormatted(filePath)
        audioItems.add(AudioItem(filePath, duration))
        audioChipAdapter.notifyItemInserted(audioItems.size - 1)
    }

    private fun getAudioDurationFormatted(filePath: String): String {
        return try {
            val mp = MediaPlayer()
            mp.setDataSource(filePath)
            mp.prepare()
            val durationMs = mp.duration
            mp.release()
            val minutes = (durationMs / 1000 / 60)
            val seconds = ((durationMs / 1000) % 60)
            String.format("%02d:%02d", minutes, seconds)
        } catch (e: Exception) {
            "00:00"
        }
    }

    private fun playPauseAudio(item: AudioItem, pos: Int) {
        if (mediaPlayer != null && currentlyPlayingIndex == pos) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            audioItems[pos] = item.copy(isPlaying = false)
            audioChipAdapter.notifyItemChanged(pos)
            currentlyPlayingIndex = null
        } else {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            audioItems.forEachIndexed { index, audioItem ->
                audioItems[index] = audioItem.copy(isPlaying = false)
            }
            audioItems[pos] = item.copy(isPlaying = true)
            audioChipAdapter.notifyDataSetChanged()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(item.filePath)
                prepare()
                start()
                setOnCompletionListener {
                    audioItems[pos] = item.copy(isPlaying = false)
                    audioChipAdapter.notifyItemChanged(pos)
                    currentlyPlayingIndex = null
                }
            }
            currentlyPlayingIndex = pos
        }
    }
} 