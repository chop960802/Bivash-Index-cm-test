package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.models.AppThemeMode
import com.example.ui.BivashIndexApp
import com.example.ui.state.BivashViewModel
import com.example.ui.theme.BivashIndexTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val bivashViewModel: BivashViewModel = viewModel()
      val themeMode by bivashViewModel.themeMode.collectAsStateWithLifecycle()
      val systemDark = isSystemInDarkTheme()
      val useDarkTheme =
        when (themeMode) {
          AppThemeMode.SYSTEM -> systemDark
          AppThemeMode.DAY -> false
          AppThemeMode.NIGHT -> true
        }

      LaunchedEffect(useDarkTheme) {
        enableEdgeToEdge(
          statusBarStyle =
            if (useDarkTheme) SystemBarStyle.dark(AndroidColor.TRANSPARENT)
            else SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
          navigationBarStyle =
            if (useDarkTheme) SystemBarStyle.dark(AndroidColor.TRANSPARENT)
            else SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
        )
      }

      BivashIndexTheme(darkTheme = useDarkTheme) {
        BivashIndexApp(
          viewModel = bivashViewModel,
          onExitApp = { finish() },
        )
      }
    }
  }
}
