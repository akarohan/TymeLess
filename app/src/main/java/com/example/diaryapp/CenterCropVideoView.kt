package com.example.diaryapp

import android.content.Context
import android.graphics.Matrix
import android.media.MediaPlayer
import android.net.Uri
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import android.widget.FrameLayout

class CenterCropVideoView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), TextureView.SurfaceTextureListener {
    private var textureView: TextureView = TextureView(context)
    private var mediaPlayer: MediaPlayer? = null
    private var videoUri: Uri? = null
    private var isLooping: Boolean = true
    private var isMuted: Boolean = true

    init {
        textureView.surfaceTextureListener = this
        addView(textureView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun setVideoURI(uri: Uri) {
        videoUri = uri
        if (textureView.isAvailable) {
            prepareMediaPlayer()
        }
    }

    fun setLooping(loop: Boolean) {
        isLooping = loop
        mediaPlayer?.isLooping = loop
    }

    fun setMuted(mute: Boolean) {
        isMuted = mute
        mediaPlayer?.setVolume(if (mute) 0f else 1f, if (mute) 0f else 1f)
    }

    private fun prepareMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(context, videoUri!!)
            setSurface(Surface(textureView.surfaceTexture))
            isLooping = this@CenterCropVideoView.isLooping
            setVolume(if (isMuted) 0f else 1f, if (isMuted) 0f else 1f)
            setOnPreparedListener {
                adjustVideoScaling()
                start()
            }
            setOnVideoSizeChangedListener { _, _, _ ->
                adjustVideoScaling()
            }
            prepareAsync()
        }
    }

    private fun adjustVideoScaling() {
        val mp = mediaPlayer ?: return
        val videoWidth = mp.videoWidth
        val videoHeight = mp.videoHeight
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (videoWidth == 0 || videoHeight == 0 || viewWidth == 0f || viewHeight == 0f) return
        val scale = Math.max(viewWidth / videoWidth, viewHeight / videoHeight)
        val scaledWidth = scale * videoWidth
        val scaledHeight = scale * videoHeight
        val dx = (viewWidth - scaledWidth) / 2
        val dy = (viewHeight - scaledHeight) / 2
        val matrix = Matrix()
        matrix.setScale(scaledWidth / viewWidth, scaledHeight / viewHeight)
        matrix.postTranslate(dx, dy)
        textureView.setTransform(matrix)
    }

    override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
        if (videoUri != null) {
            prepareMediaPlayer()
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
        adjustVideoScaling()
    }

    override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
        mediaPlayer?.release()
        mediaPlayer = null
        return true
    }

    override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
} 