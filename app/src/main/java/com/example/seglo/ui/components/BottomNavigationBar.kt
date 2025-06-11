package com.example.seglo.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.seglo.ui.theme.LocalCustomColors

sealed class BottomNavItem(val route: String, val icon: ImageVector, val title: String) {
    object Home : BottomNavItem("home", Icons.Default.Home, "Home")
    object Camera : BottomNavItem("camera", Icons.Default.Camera, "Camera")
    object Settings : BottomNavItem("settings", Icons.Default.Settings, "Settings")
}

@Composable
fun BottomNavigationBar(
    selectedRoute: String,
    onItemSelected: (String) -> Unit
) {
    val customColors = LocalCustomColors.current
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Camera,
        BottomNavItem.Settings
    )

    NavigationBar(
        containerColor = customColors.softGray,
        contentColor = customColors.darkGray,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = if (selectedRoute == item.route) customColors.darkPurple else customColors.darkGray.copy(alpha = 0.6f)
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        fontSize = 12.sp,
                        color = if (selectedRoute == item.route) customColors.darkPurple else customColors.darkGray.copy(alpha = 0.6f)
                    )
                },
                selected = selectedRoute == item.route,
                onClick = { onItemSelected(item.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = customColors.darkPurple,
                    selectedTextColor = customColors.darkPurple,
                    unselectedIconColor = customColors.darkGray.copy(alpha = 0.6f),
                    unselectedTextColor = customColors.darkGray.copy(alpha = 0.6f),
                    indicatorColor = customColors.darkPurple.copy(alpha = 0.1f)
                )
            )
        }
    }
}