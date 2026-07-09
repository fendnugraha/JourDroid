package com.example.jourdroid.ui.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.jourdroid.api.ApiClient
import com.example.jourdroid.data.UserData
import com.example.jourdroid.utils.AuthManager
import kotlinx.coroutines.launch
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton

@Composable
fun LoginScreen(onLoginSuccess: (UserData) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authManager = remember { AuthManager(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Welcome to Jourdroid", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // ... Input Email & Password tetap sama ...
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        // ================= TOMBOL LOGIN UTAMA =================
        Button(
            onClick = {
                scope.launch {
                    try {
                        val apiService = ApiClient.getApiService(context)
                        val response = apiService.login(email, password)

                        if (response.success && response.token != null) {
                            authManager.saveToken(response.token)

                            // 🔴 AMBIL OBJECT USER DARI LOGINRESPONSE DAN SIMPAN KE HP
                            response.user?.let { user ->
                                authManager.saveUserData(user)
                                Toast.makeText(context, "Login Sukses! Token disimpan.", Toast.LENGTH_SHORT).show()
                                onLoginSuccess(user)
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Koneksi Gagal: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ================= 🔴 TOMBOL TEST KONEKSI BARU =================
        OutlinedButton(
            onClick = {
                scope.launch {
                    try {
                        Toast.makeText(context, "Mencoba menghubungkan ke server...", Toast.LENGTH_SHORT).show()

                        val apiService = ApiClient.getApiService(context)
                        val response = apiService.testConnection()

                        if (response.isSuccessful) {
                            Toast.makeText(context, "⚡ Terhubung! Server Laravel & Ngrok Aktif.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "⚠️ Server merespon, tapi status: ${response.code()}", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        // Jika URL Ngrok salah atau Ngrok mati, akan langsung masuk ke sini
                        Toast.makeText(context, "❌ Gagal Terhubung! Periksa kembali URL Ngrok di ApiClient.kt atau jalankan ngrok di Macbook.", Toast.LENGTH_LONG).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Test Koneksi ke API")
        }
    }
}