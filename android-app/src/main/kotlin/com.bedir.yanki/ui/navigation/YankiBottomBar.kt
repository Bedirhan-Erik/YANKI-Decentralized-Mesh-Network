package com.bedir.yanki.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.bedir.yanki.ui.navigation.Screen
import com.bedir.yanki.ui.navigation.bottomNavItems
import com.bedir.yanki.ui.theme.*

@Composable
fun YankiBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = YankiDarkBg, // Tasarımdaki koyu arka plan
        tonalElevation = 8.dp
    ) {
        bottomNavItems.forEach { screen ->
            val isSelected = currentRoute == screen.route

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            // Menü geçişlerinde stack birikmesini önler
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.title,
                        tint = if (isSelected) YankiGreen else YankiGreyDot
                    )
                },
                label = {
                    Text(
                        text = screen.title,
                        color = if (isSelected) YankiGreen else YankiGreyDot,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = YankiCardBg // Seçili ikonun arkasındaki hafif parlayan yuvarlak
                )
            )
        }
    }
}