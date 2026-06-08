package com.bedir.yanki.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Welcome : Screen("welcome", "Hoş Geldiniz", Icons.Default.Home)
    object RegisterStep1 : Screen("register_step1", "Hesap Oluştur", Icons.Default.Person)
    object RegisterStep2 : Screen("register_step2", "Sağlık Bilgileri", Icons.Default.Favorite)
    object RegisterStep3 : Screen("register_step3", "Acil Durum Yakını", Icons.Default.Call)
    object RegisterSuccess : Screen("register_success", "Hesap Hazır", Icons.Default.CheckCircle)
    object Login : Screen("login", "Giriş Yap", Icons.Default.Lock)
    object HealthDetail : Screen("health_detail", "Sağlık Bilgileri", Icons.Default.Favorite)
    object EditHealth : Screen("edit_health/{type}", "Düzenle", Icons.Default.Edit) {
        fun createRoute(type: String) = "edit_health/$type"
    }
    object Settings : Screen("settings", "Ayarlar", Icons.Default.Settings)

    object Home : Screen("home", "Ana", Icons.Default.Home)
    object Network : Screen("network", "Ağ", Icons.Default.Warning)
    object Messages : Screen("messages", "Mesaj", Icons.Default.MailOutline)
    object Profile : Screen("profile", "Profil", Icons.Default.Person)
    object BulletinBoard : Screen("bulletin", "Duyurular", Icons.AutoMirrored.Filled.Announcement)

    object ChatDetail : Screen("chat/{userId}?userName={userName}", "Sohbet", Icons.Default.MailOutline) {
        fun createRoute(userId: String, userName: String = "Bilinmeyen") = "chat/$userId?userName=$userName"
    }
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Network,
    Screen.BulletinBoard,
    Screen.Messages,
    Screen.Profile
)