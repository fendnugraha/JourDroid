package com.example.jourdroid.ui.app

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.jourdroid.data.UserData
import com.example.jourdroid.ui.app.dashboard.DashboardScreen
import com.example.jourdroid.ui.app.transaction.TransactionScreen
import com.example.jourdroid.ui.app.profile.ProfileScreen

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
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // Disable default insets to let screens handle them
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                val items = listOf(Screen.Dashboard, Screen.Transactions, Screen.Profile)
                items.forEach { screen ->
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
        // innerPadding only contains bottom padding for NavigationBar due to contentWindowInsets=0
        Crossfade(
            targetState = currentScreen,
            modifier = Modifier.padding(innerPadding),
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                is Screen.Dashboard -> DashboardScreen(user = user)
                is Screen.Transactions -> TransactionScreen(user = user)
                is Screen.Profile -> ProfileScreen(user = user, onLogoutClick = onLogoutClick)
            }
        }
    }
}
