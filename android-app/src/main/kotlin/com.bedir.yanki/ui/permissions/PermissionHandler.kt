package com.bedir.yanki.ui.permissions

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun PermissionHandler(onPermissionsGranted: @Composable () -> Unit) {
    val context = LocalContext.current
    var allPermissionsGranted by remember { mutableStateOf(false) }

    // İzinleri kontrol eden yardımcı fonksiyon
    fun checkPermissions(): Boolean {
        return PermissionConstants.meshPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Uygulama açıldığında ilk kontrolü yap
    LaunchedEffect(Unit) {
        allPermissionsGranted = checkPermissions()
    }

    // İzin İsteme Fırlatıcısı
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        // Tüm izinler true döndü mü kontrol et
        allPermissionsGranted = permissionsMap.values.all { it }
    }

    if (allPermissionsGranted) {
        // İzinler verildiyse uygulamanın asıl ekranını (TestScreen vb.) göster
        onPermissionsGranted()
    } else {
        // İzinler eksikse kullanıcıyı ikna etme ekranı göster
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "YANKI'nın Çalışması İçin İzinlere İhtiyacı Var",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Afet anında diğer cihazlarla bağlantı kurabilmemiz için Bluetooth, Wi-Fi ve Konum izinlerini onaylamanız gerekmektedir.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(PermissionConstants.meshPermissions) }
                ) {
                    Text("İzinleri Ver")
                }
            }
        }
    }
}