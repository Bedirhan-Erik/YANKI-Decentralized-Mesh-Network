package com.bedir.yanki.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bedir.yanki.ui.theme.*
import com.bedir.yanki.ui.viewmodel.MeshViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: MeshViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showKeyDialog by remember { mutableStateOf(false) }

    val keyAgeDays = remember { viewModel.getKeyAgeDays() }
    val keyAgeText = when {
        keyAgeDays < 0L -> "Henüz oluşturulmadı"
        keyAgeDays == 0L -> "Bugün oluşturuldu"
        else -> "$keyAgeDays gün önce"
    }

    if (showKeyDialog) {
        AlertDialog(
            onDismissRequest = { showKeyDialog = false },
            title = { Text("Anahtar Yenile", fontWeight = FontWeight.Bold) },
            text = { Text("Mevcut Ed25519 anahtar çifti silinecek ve yenisi oluşturulacak. Devam edilsin mi?") },
            confirmButton = {
                Button(
                    onClick = {
                        showKeyDialog = false
                        viewModel.regenerateKeys { success ->
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (success) "Anahtar başarıyla yenilendi" else "Anahtar yenilenemedi"
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = YankiGreen)
                ) { Text("Yenile", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showKeyDialog = false }) { Text("İptal") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayarlar", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = YankiDarkBg)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = YankiDarkBg
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(YankiDarkBg)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection(title = "GÜVENLİK") {
                SettingsSwitchItem(
                    icon = Icons.Default.Lock,
                    title = "Android Keystore",
                    subtitle = "Donanım güvenlik modülü",
                    checked = true,
                    onCheckedChange = {},
                    enabled = false,
                    showBadge = true,
                    badgeText = "Aktif"
                )
                SettingsSwitchItem(
                    icon = Icons.Default.Settings,
                    title = "Mesaj İmzalama",
                    subtitle = "Ed25519 ile gönderilen mesajları imzala",
                    checked = settings["pref_aes_gcm"] ?: true,
                    onCheckedChange = { viewModel.updateSetting("pref_aes_gcm", it) }
                )
                SettingsClickItem(
                    icon = Icons.Default.Edit,
                    title = "Anahtar yenile",
                    subtitle = "Son: $keyAgeText",
                    onClick = { showKeyDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSection(title = "AĞ TERCİHLERİ") {
                SettingsSwitchItem(
                    icon = Icons.Default.Share,
                    title = "Wi-Fi Aware (NAN)",
                    subtitle = "Yüksek hızlı bağlantı",
                    checked = settings["pref_wifi_aware"] ?: true,
                    onCheckedChange = { viewModel.updateSetting("pref_wifi_aware", it) }
                )
                SettingsSwitchItem(
                    icon = Icons.Default.Share,
                    title = "BLE Modu",
                    subtitle = "Düşük enerji keşif",
                    checked = settings["pref_ble_mode"] ?: true,
                    onCheckedChange = { viewModel.updateSetting("pref_ble_mode", it) }
                )
                SettingsSwitchItem(
                    icon = Icons.Default.CheckCircle,
                    title = "Güvenilir cihaz modu",
                    subtitle = "Yalnızca güvenilir kişilerden mesaj al",
                    checked = settings["pref_trusted_only"] ?: false,
                    onCheckedChange = { viewModel.updateSetting("pref_trusted_only", it) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSection(title = "BİLDİRİMLER") {
                SettingsSwitchItem(
                    icon = Icons.Default.Notifications,
                    title = "Acil sinyal uyarıları",
                    subtitle = "Yeni SOS geldiğinde bildirim gönder",
                    checked = settings["pref_sos_notifications"] ?: true,
                    onCheckedChange = { viewModel.updateSetting("pref_sos_notifications", it) }
                )
                SettingsSwitchItem(
                    icon = Icons.Default.LocationOn,
                    title = "Yeni cihaz keşfi",
                    subtitle = "Mesh'e yeni komşu katıldığında bildirim gönder",
                    checked = settings["pref_discovery_notifications"] ?: false,
                    onCheckedChange = { viewModel.updateSetting("pref_discovery_notifications", it) }
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
            color = YankiGreyDot,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = YankiCardBg),
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
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    showBadge: Boolean = false,
    badgeText: String = ""
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
                .background(YankiDarkBg, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = YankiGreen, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, fontWeight = FontWeight.Bold, color = Color.White)
                if (showBadge) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = YankiGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = YankiGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(text = subtitle, color = YankiGreyDot, fontSize = 12.sp)
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = YankiGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = YankiGreyDot,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun SettingsClickItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(YankiDarkBg, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = YankiGreen, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = subtitle, color = YankiGreyDot, fontSize = 12.sp)
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = YankiGreyDot
        )
    }
}
