package com.example.diaryapp

import android.app.Activity
import androidx.core.content.ContextCompat

object ThemeUtils {
    /**
     * Get the current theme color
     */
    fun getCurrentThemeColor(activity: Activity): Int {
        val prefsColor = activity.getSharedPreferences("theme_prefs", Activity.MODE_PRIVATE)
        val defaultColor = ContextCompat.getColor(activity, R.color.greyback)
        return prefsColor.getInt("theme_color", defaultColor)
    }
} 