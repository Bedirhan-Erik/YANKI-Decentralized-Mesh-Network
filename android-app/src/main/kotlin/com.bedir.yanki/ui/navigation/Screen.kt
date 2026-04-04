package com.bedir.yanki.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Ana", Icons.Default.Home)
    object Network : Screen("network", "Ağ", Icons.Default.Warning)
    object Messages : Screen("messages", "Mesaj", Icons.Default.MailOutline)
    object Profile : Screen("profile", "Profil", Icons.Default.Person)

    object ChatDetail : Screen("chat/{userId}?userName={userName}", "Sohbet", Icons.Default.MailOutline) {
        fun createRoute(userId: String, userName: String = "Bilinmeyen") = "chat/$userId?userName=$userName"
    }
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Network,
    Screen.Messages,
    Screen.Profile
)