package com.example.flashcards

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.core.content.edit

/** The app's light or dark appearance. */
enum class ThemeMode(val label: String) {
  Light("Light"),
  Dark("Dark"),
  /** Follows the phone's own light or dark setting. */
  System("System"),
}

/** Whether this mode is dark, given whether the phone itself is in dark mode. */
fun ThemeMode.isDark(systemDark: Boolean): Boolean =
  when (this) {
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
    ThemeMode.System -> systemDark
  }

/** App-wide settings, kept in shared preferences. */
class SettingsStore(private val context: Context) {
  private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

  /** Light unless the user has chosen otherwise. */
  var themeMode: ThemeMode
    get() =
      prefs.getString(THEME_KEY, null)?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } }
        ?: ThemeMode.Light
    set(mode) {
      prefs.edit { putString(THEME_KEY, mode.name) }
      shareThemeWithSystem(mode)
    }

  /**
   * On Android 12 and later, tells the system the app's own light or dark choice, so what the system
   * draws before the app's first frame (the launch screen) matches it instead of the phone's
   * setting. Earlier versions have no per-app setting: there the launch screen follows the phone,
   * and the app switches to the chosen theme as soon as it draws.
   */
  fun shareThemeWithSystem(mode: ThemeMode = themeMode) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val uiModes = context.getSystemService(UiModeManager::class.java) ?: return
    uiModes.setApplicationNightMode(
      when (mode) {
        ThemeMode.Light -> UiModeManager.MODE_NIGHT_NO
        ThemeMode.Dark -> UiModeManager.MODE_NIGHT_YES
        // Clears the app's override, so it takes the phone's setting.
        ThemeMode.System -> UiModeManager.MODE_NIGHT_AUTO
      }
    )
  }

  private companion object {
    const val THEME_KEY = "theme"
  }
}
