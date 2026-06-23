package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      var isDarkMode by remember { mutableStateOf(true) }
      MyApplicationTheme(darkTheme = isDarkMode, dynamicColor = false) {
        var isLoggedIn by remember { mutableStateOf(false) }

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          if (isLoggedIn) {
            DashboardScreen(
              onLogout = { isLoggedIn = false },
              isDarkMode = isDarkMode,
              onToggleTheme = { isDarkMode = !isDarkMode },
              modifier = Modifier.fillMaxSize()
            )
          } else {
            LoginScreen(
              onLoginSuccess = { isLoggedIn = true },
              isDarkMode = isDarkMode,
              onToggleTheme = { isDarkMode = !isDarkMode },
              modifier = Modifier.fillMaxSize()
            )
          }
        }
      }
    }
  }
}
