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
import com.example.jourdroid.data.UserData
import com.example.jourdroid.ui.app.MainAppContainer
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
                var isLoggedIn by remember {
                    mutableStateOf(!authManager.getToken().isNullOrEmpty())
                }

                // 2. 🟢 INI DIA! State untuk mengambil data objek user dari AuthManager
                var currentUser by remember {
                    mutableStateOf<UserData?>(authManager.getUserData())
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // CONDITIONAL RENDERING (Saklar manual bolak-balik)
                        val user = currentUser

                        if (isLoggedIn && user != null) {
                            MainAppContainer(
                                user = user,
                                onLogoutClick = {
                                    authManager.clearAuth()
                                    currentUser = null
                                    isLoggedIn = false
                                    Toast.makeText(this@MainActivity, "Logout Berhasil", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            LoginScreen(
                                onLoginSuccess = { userResponse ->
                                    // Saat login sukses, tangkap objek userResponse, lalu simpan ke HP dan update state
                                    authManager.saveUserData(userResponse)
                                    currentUser = userResponse
                                    isLoggedIn = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}