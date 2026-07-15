package com.example.jourdroid

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.jourdroid.api.ApiClient
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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

                // Permission Launcher
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (!isGranted) {
                        Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
                    }
                }

                // Request permission on start if needed
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                // Sync FCM Token when logged in
                LaunchedEffect(isLoggedIn) {
                    if (isLoggedIn) {
                        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                            if (!task.isSuccessful) {
                                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                                return@addOnCompleteListener
                            }

                            val token = task.result
                            Log.d("FCM", "FCM Token: $token")
                            
                            // For debugging only: show token or status
                            // Toast.makeText(this@MainActivity, "FCM Token retrieved", Toast.LENGTH_SHORT).show()
                            
                            // Send token to backend
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val response = ApiClient.getApiService(this@MainActivity).updateFcmToken(token)
                                    if (response.isSuccessful) {
                                        Log.d("FCM", "Token updated successfully")
                                    } else {
                                        val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                                        Log.e("FCM", "Token update failed: ${response.code()} $errorMsg")
                                        // Optional: show toast on main thread if you want to notify user
                                    }
                                } catch (e: Exception) {
                                    Log.e("FCM", "Failed to update token on server", e)
                                }
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
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