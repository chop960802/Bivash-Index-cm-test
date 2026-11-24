package com.example.storage

import android.content.Context
import com.example.models.AppThemeMode

class ThemePreferences(context: Context) {
  private val prefs =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun getThemeMode(): AppThemeMode {
    val saved = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
    return AppThemeMode.entries.firstOrNull { it.name == saved } ?: AppThemeMode.SYSTEM
  }

  fun setThemeMode(mode: AppThemeMode) {
    prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
  }

  private companion object {
    const val PREFS_NAME = "bivash_ui_prefs"
    const val KEY_THEME_MODE = "theme_mode"
  }
}
