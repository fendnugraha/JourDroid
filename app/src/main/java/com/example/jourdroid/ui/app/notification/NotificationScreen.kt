package com.example.jourdroid.ui.app.notification

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jourdroid.api.ApiClient
import com.example.jourdroid.data.NotificationItem
import com.example.jourdroid.utils.FormatterUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiService = remember { ApiClient.getApiService(context) }

    var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var unreadCount by remember { mutableStateOf(0) }

    val fetchNotifications = suspend {
        try {
            isLoading = true
            val response = apiService.getNotifications(page = 1)
            if (response.success) {
                notifications = response.data.data
                unreadCount = response.unreadCount
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal memuat notifikasi", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
            isRefreshing = false
        }
    }

    val markAsRead = { id: String ->
        scope.launch {
            try {
                val response = apiService.markNotificationRead(id)
                if (response.isSuccessful) {
                    notifications = notifications.map {
                        if (it.id == id) it.copy(readAt = "read") else it
                    }
                    unreadCount = (unreadCount - 1).coerceAtLeast(0)
                }
            } catch (e: Exception) {
                // Ignore error
            }
        }
    }

    val markAllAsRead = {
        scope.launch {
            try {
                val response = apiService.markAllNotificationsRead()
                if (response.isSuccessful) {
                    notifications = notifications.map { it.copy(readAt = "read") }
                    unreadCount = 0
                    Toast.makeText(context, "Semua ditandai telah dibaca", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal menandai semua", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchNotifications()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifikasi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        IconButton(onClick = { markAllAsRead() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Mark all read")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { 
                isRefreshing = true
                scope.launch { fetchNotifications() } 
            },
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            if (notifications.isEmpty() && !isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.NotificationsNone, 
                            null, 
                            modifier = Modifier.size(64.dp), 
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Belum ada notifikasi", color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(notifications) { item ->
                        NotificationRow(
                            item = item,
                            onClick = { 
                                if (!item.isRead) markAsRead(item.id) 
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationRow(
    item: NotificationItem,
    onClick: () -> Unit
) {
    val backgroundColor = if (item.isRead) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (item.isRead) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (item.isRead) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (item.isRead) FontWeight.Medium else FontWeight.Bold,
                    color = if (item.isRead) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                )
                
                if (!item.isRead) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
            }
            
            Text(
                text = item.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            Text(
                text = item.createdAt?.let { FormatterUtils.formatShortDate(it) } ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
