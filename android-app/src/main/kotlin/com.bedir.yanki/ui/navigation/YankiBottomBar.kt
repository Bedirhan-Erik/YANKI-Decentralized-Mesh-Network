package com.bedir.yanki.ui.navigation

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.bedir.yanki.ui.theme.*

@Composable
fun YankiBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        containerColor = YankiDarkBg,
        tonalElevation = 8.dp
    ) {
        bottomNavItems.forEach { screen ->
            // Hiyerarşi kontrolü: Alt sayfaların hangi sekmeye ait olduğunu anlar (örn: Mesaj detayı -> Mesajlar sekmesi seçili kalır)
            val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        navController.navigate(screen.route) {
                            // Ana hedefe dönerken stack temizliği yapar
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
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
                    indicatorColor = YankiCardBg.copy(alpha = 0.5f)
                )
            )
        }
    }
}
