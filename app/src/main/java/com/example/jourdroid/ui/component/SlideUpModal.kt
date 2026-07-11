package com.example.jourdroid.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlideUpModal(
    visible: Boolean,
    title: String,
    onClose: () -> Unit,
    content: @Composable () -> Unit // Slot dinamis untuk isi konten ("div" kamu)
) {
    // Mengatur animasi slide dari bawah ke atas (dan sebaliknya saat keluar)
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }), // Geser naik dari bawah layar
        exit = slideOutVertically(targetOffsetY = { it })   // Geser turun ke bawah layar
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background // Menutupi layar secara penuh
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(title) },
                        navigationIcon = {
                            // Tombol silang untuk menutup modal
                            IconButton(onClick = onClose) {
                                Icon(Icons.Default.Close, contentDescription = "Tutup")
                            }
                        }
                    )
                }
            ) { innerPadding ->
                // Wadah isi konten dinamis yang sudah diberi padding aman dari TopBar
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding) // 🟢 Pindahkan innerPadding ke Modifier seperti ini!
                ) {
                    content() // Isi konten dinamis tetap di sini
                }
            }
        }
    }
}