package com.example.jourdroid.ui.app

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.jourdroid.api.ApiClient
import com.example.jourdroid.data.UserData
import com.example.jourdroid.ui.app.dashboard.DashboardScreen
import com.example.jourdroid.ui.app.transaction.TransactionScreen
import com.example.jourdroid.ui.app.profile.ProfileScreen
import com.example.jourdroid.ui.app.delivery.DeliveryScreen
import com.example.jourdroid.ui.app.task.TaskScreen
import com.example.jourdroid.ui.app.pos.SalesScreen
import com.example.jourdroid.ui.app.attendance.AttendanceScreen
import com.example.jourdroid.ui.app.attendance.AttendanceSuccessScreen
import com.example.jourdroid.data.AttendanceData
import kotlinx.coroutines.launch

sealed class Screen(
    val route: String, 
    val label: String, 
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Dashboard : Screen(
        "dashboard", 
        "Home", 
        Icons.Filled.Dashboard, 
        Icons.Outlined.Dashboard
    )
    object Task : Screen(
        "task",
        "Tugas",
        Icons.Default.Assignment,
        Icons.Outlined.Assignment
    )
    object Delivery : Screen(
        "delivery",
        "Pengiriman",
        Icons.Filled.LocalShipping,
        Icons.Outlined.LocalShipping
    )
    object Transactions : Screen(
        "transactions", 
        "Activity",
        Icons.AutoMirrored.Filled.ReceiptLong,
        Icons.AutoMirrored.Outlined.ReceiptLong
    )
    object POS : Screen(
        "pos",
        "POS",
        Icons.Filled.ShoppingCart,
        Icons.Outlined.ShoppingCart
    )
    object Profile : Screen(
        "profile", 
        "Profile", 
        Icons.Filled.AccountCircle, 
        Icons.Outlined.AccountCircle
    )
}

@Composable
fun MainAppContainer(
    user: UserData,
    onLogoutClick: () -> Unit
) {
    var currentUser by remember { mutableStateOf(user) }
    val scope = rememberCoroutineScope()
    val userRole = currentUser.role?.toString() ?: ""
    val isCourier = userRole.equals("Courier", ignoreCase = true)
    
    // Bypass attendance for Admins
    val isAdmin = listOf("Administrator", "Super Admin")
        .any { it.equals(userRole, ignoreCase = true) }
    
    var hasCheckedInState by remember { mutableStateOf(currentUser.hasCheckedIn == true) }
    var isNavigatingToAttendance by remember { mutableStateOf(false) }
    var attendanceSuccessData by remember { mutableStateOf<AttendanceData?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val apiService = ApiClient.getApiService(context)
                val statusResponse = apiService.getUserCheckedInStatus()
                hasCheckedInState = statusResponse.hasCheckedIn
            } catch (e: Exception) {
                // Keep current state on error
            }
        }
    }

    // Define navigation items based on role
    val navigationItems = remember(userRole) {
        val list = mutableListOf<Screen>()
        
        // Dashboard screen for all except Courier
        val canSeeDashboard = !isCourier
        if (canSeeDashboard) {
            list.add(Screen.Dashboard)
        }
        
        // Task screen for courier or admins
        if (isCourier) {
            list.add(Screen.Task)
        }

        // Delivery only show for role Administrator, Super Admin and Courier
        val canSeeDelivery = listOf("Administrator", "Super Admin", "Courier")
            .any { it.equals(userRole, ignoreCase = true) }
        if (canSeeDelivery) {
            list.add(Screen.Delivery)
        }

        // POS screen for non-courier
        if (listOf("Cashier", "Co-Cashier")
                .any { it.equals(userRole, ignoreCase = true) }) {
            list.add(Screen.POS)
        }
        
        // Activity (Transactions) is for everyone EXCEPT Courier
        if (!isCourier) {
            list.add(Screen.Transactions)
        }
        
        // Profile is common for all
        list.add(Screen.Profile)
        list
    }

    var currentScreen by remember(userRole) { 
        mutableStateOf(
            if (isCourier) {
                if (navigationItems.contains(Screen.Task)) Screen.Task else Screen.Delivery
            } else if (navigationItems.contains(Screen.Dashboard)) {
                Screen.Dashboard
            } else {
                navigationItems.firstOrNull() ?: Screen.Profile
            }
        ) 
    }

    if (attendanceSuccessData != null) {
        AttendanceSuccessScreen(
            userName = currentUser.name ?: "User",
            attendanceData = attendanceSuccessData!!,
            onDone = { attendanceSuccessData = null }
        )
    } else if (isNavigatingToAttendance) {
        AttendanceScreen(
            user = currentUser,
            onSuccess = { data ->
                attendanceSuccessData = data
                hasCheckedInState = true
                isNavigatingToAttendance = false
            },
            onLogout = {
                isNavigatingToAttendance = false
                onLogoutClick()
            }
        )
    } else {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    navigationItems.forEach { screen ->
                        val isSelected = currentScreen == screen
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.label,
                                    modifier = Modifier.size(26.dp)
                                )
                            },
                            label = {
                                Text(
                                    screen.label,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            selected = isSelected,
                            onClick = { currentScreen = screen },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Crossfade(
                targetState = currentScreen,
                modifier = Modifier.padding(innerPadding),
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    is Screen.Dashboard -> DashboardScreen(
                        user = currentUser,
                        hasCheckedIn = hasCheckedInState,
                        onNavigateToAttendance = {
                            scope.launch {
                                try {
                                    val statusResponse = ApiClient.getApiService(context).getUserCheckedInStatus()
                                    val hasCheckedIn = statusResponse.hasCheckedIn
                                    hasCheckedInState = hasCheckedIn
                                    if (!hasCheckedIn) {
                                        isNavigatingToAttendance = true
                                    } else {
                                        android.widget.Toast.makeText(context, "Sudah Absen", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    // Fallback if API fails, just show the screen
                                    isNavigatingToAttendance = true
                                }
                            }
                        }
                    )
                    is Screen.Task -> TaskScreen(user = currentUser)
                    is Screen.Delivery -> DeliveryScreen(user = currentUser)
                    is Screen.POS -> SalesScreen(user = currentUser)
                    is Screen.Transactions -> TransactionScreen(user = currentUser)
                    is Screen.Profile -> ProfileScreen(
                        user = currentUser, 
                        onLogoutClick = onLogoutClick,
                        onUserUpdate = { newUser -> 
                            currentUser = newUser
                            hasCheckedInState = newUser.hasCheckedIn == true
                        }
                    )
                }
            }
        }
    }
}
