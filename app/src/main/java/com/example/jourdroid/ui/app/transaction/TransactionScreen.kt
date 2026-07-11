package com.example.jourdroid.ui.app.transaction

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(){
    Scaffold(
        topBar = {
            // 1. Tempat untuk Header / Navbar Atas (misal: judul "Jourdroid")
            TopAppBar(title = { Text("Jourdroid") })
        },
        bottomBar = {
            // 2. Tempat untuk Menu Navigasi Bawah (Bottom Navigation)
            BottomAppBar { /* Icon menu disini */ }
        },
        floatingActionButton = {
            // 3. Tempat untuk Tombol Bulat Melayang (misal: tombol tambah jurnal)
            FloatingActionButton(onClick = { /* tambah data */ }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { innerPadding ->
        // 4. Tempat untuk Isi Utama Halaman Kamu (Main Content)
        // innerPadding otomatis menjaga agar isi konten tidak tertutup oleh TopBar atau BottomBar
        Column(modifier = Modifier.padding(innerPadding)) {
            Text("Isi Halaman Kamu Disini")
        }
    }
}