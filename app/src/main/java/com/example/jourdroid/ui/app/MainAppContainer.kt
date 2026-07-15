package com.example.jourdroid.ui.app

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.jourdroid.data.UserData
import com.example.jourdroid.ui.app.dashboard.DashboardScreen
import com.example.jourdroid.ui.app.transaction.TransactionScreen
import com.example.jourdroid.ui.app.profile.ProfileScreen
import com.example.jourdroid.ui.app.delivery.DeliveryScreen

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
    object Delivery : Screen(
        "delivery",
        "Delivery",
        Icons.Filled.LocalShipping,
        Icons.Outlined.LocalShipping
    )
    object Transactions : Screen(
        "transactions", 
        "Activity",
        Icons.AutoMirrored.Filled.ReceiptLong,
        Icons.AutoMirrored.Outlined.ReceiptLong
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
    val userRole = user.role?.role ?: ""
    val isCourier = userRole.equals("Courier", ignoreCase = true)
    
    // Define navigation items based on role
    val navigationItems = remember(userRole) {
        val list = mutableListOf<Screen>()
        
        // Show Dashboard only for specific roles
        val canSeeDashboard = listOf("Administrator", "Super Admin", "Staff", "Cashier")
            .any { it.equals(userRole, ignoreCase = true) }
            
        if (canSeeDashboard) {
            list.add(Screen.Dashboard)
        }
        
        // Show Delivery only for Courier
        if (isCourier) {
            list.add(Screen.Delivery)
        }
        
        // Activity and Profile are common
        list.add(Screen.Transactions)
        list.add(Screen.Profile)
        list
    }

    var currentScreen by remember { 
        mutableStateOf(if (isCourier) Screen.Delivery else if (navigationItems.contains(Screen.Dashboard)) Screen.Dashboard else Screen.Transactions) 
    }

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
                is Screen.Dashboard -> DashboardScreen(user = user)
                is Screen.Delivery -> DeliveryScreen(user = user)
                is Screen.Transactions -> TransactionScreen(user = user)
                is Screen.Profile -> ProfileScreen(user = user, onLogoutClick = onLogoutClick)
            }
        }
    }
}
