package com.example.jourdroid

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.jourdroid.ui.app.dashboard.DashboardScreen
import com.example.jourdroid.ui.auth.LoginScreen
import com.example.jourdroid.ui.theme.JourDroidTheme
import com.example.jourdroid.utils.AuthManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authManager = AuthManager(this)

        setContent {
            JourDroidTheme {
                // State lokal untuk menentukan halaman mana yang aktif (Seperti di React)
                var isLoggedIn by remember {
                    mutableStateOf(!authManager.getToken().isNullOrEmpty())
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // CONDITIONAL RENDERING (Saklar manual bolak-balik)
                        if (isLoggedIn) {
                            DashboardScreen(
                                userName = authManager.getUserName(),   // 🔴 Ambil nama dari memori HP
                                userEmail = authManager.getUserEmail(), // 🔴 Ambil email dari memori HP
                                onLogoutClick = {
                                    authManager.clearAuth()
                                    Toast.makeText(this@MainActivity, "Logout Berhasil", Toast.LENGTH_SHORT).show()
                                    isLoggedIn = false
                                }
                            )
                        } else {
                            LoginScreen(onLoginSuccess = { isLoggedIn = true })
                        }
                    }
                }
            }
        }
    }
}