package com.bedir.yanki.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayarlar", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8F9FA))
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection(title = "GÜVENLİK") {
                SettingsSwitchItem(
                    icon = Icons.Default.Lock,
                    title = "Android Keystore",
                    subtitle = "Donanım güvenlik modülü",
                    initialValue = true,
                    showBadge = true,
                    badgeText = "Aktif"
                )
                SettingsSwitchItem(
                    icon = Icons.Default.Settings,
                    title = "AES-GCM Şifreleme",
                    subtitle = "Google Tink",
                    initialValue = false
                )
                SettingsClickItem(
                    icon = Icons.Default.Edit,
                    title = "Anahtar yenile",
                    subtitle = "Son: 14 gün önce"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSection(title = "AĞ TERCİHLERİ") {
                SettingsSwitchItem(
                    icon = Icons.Default.Wifi,
                    title = "Wi-Fi Aware (NAN)",
                    subtitle = "Yüksek hızlı bağlantı",
                    initialValue = true
                )
                SettingsSwitchItem(
                    icon = Icons.Default.Share,
                    title = "BLE Modu",
                    subtitle = "Düşük enerji keşif",
                    initialValue = true
                )
                SettingsSwitchItem(
                    icon = Icons.Default.CheckCircle,
                    title = "Güvenilir cihaz modu",
                    subtitle = "Yalnızca is_trusted kişiler",
                    initialValue = false
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSection(title = "BİLDİRİMLER") {
                SettingsSwitchItem(
                    icon = Icons.Default.Notifications,
                    title = "Acil sinyal uyarıları",
                    subtitle = "Yeni SOS geldiğinde",
                    initialValue = true
                )
                SettingsSwitchItem(
                    icon = Icons.Default.LocationOn,
                    title = "Yeni cihaz keşfi",
                    subtitle = "Mesh'e yeni katılan",
                    initialValue = false
                )
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            color = Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    initialValue: Boolean,
    showBadge: Boolean = false,
    badgeText: String = ""
) {
    var checked by remember { mutableStateOf(initialValue) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF111721), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, fontWeight = FontWeight.Bold, color = Color.Black)
                if (showBadge) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color(0xFF2ECC71).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = Color(0xFF2ECC71),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(text = subtitle, color = Color.Gray, fontSize = 12.sp)
        }

        Switch(
            checked = checked,
            onCheckedChange = { checked = it },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2ECC71),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.LightGray,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun SettingsClickItem(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF111721).copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF111721), modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, color = Color.Black)
            Text(text = subtitle, color = Color.Gray, fontSize = 12.sp)
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.LightGray
        )
    }
}
