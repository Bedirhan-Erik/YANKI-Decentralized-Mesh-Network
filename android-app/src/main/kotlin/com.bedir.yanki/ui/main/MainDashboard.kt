package com.bedir.yanki.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bedir.yanki.ui.theme.*
import com.bedir.yanki.ui.viewmodel.MeshViewModel
import com.bedir.yanki.ui.radar.RadarView
import com.bedir.yanki.ui.main.components.StatItem

@Composable
fun MainDashboard(viewModel: MeshViewModel = hiltViewModel()) {
    val meshUiState by viewModel.meshStatus.collectAsState()

    // Dashboard artık kendi Scaffold'una sahip değil, sadece Box/Column
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(YankiDarkBg) // Koyu arka plan burada veriliyor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = YankiCardBg),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Mesh Aktif/Kapalı Yazısı
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text(
                            text = if (meshUiState.isMeshActive) "Mesh aktif" else "Mesh kapalı",
                            color = if (meshUiState.isMeshActive) YankiGreen else YankiGreyDot
                        )
                    }

                    // Radar Görünümü
                    RadarView(
                        isMeshActive = meshUiState.isMeshActive,
                        neighborCount = meshUiState.neighborCount,
                        modifier = Modifier.size(250.dp).align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // İstatistik Kartları
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatItem(value = "${meshUiState.neighborCount}", label = "Cihaz", valueColor = YankiGreen, modifier = Modifier.weight(1f))
                        Divider(modifier = Modifier.height(40.dp).width(1.dp), color = Color.Gray.copy(alpha = 0.3f))
                        StatItem(value = meshUiState.coverageRange, label = "Kapsama", modifier = Modifier.weight(1f))
                        Divider(modifier = Modifier.height(40.dp).width(1.dp), color = Color.Gray.copy(alpha = 0.3f))
                        StatItem(
                            value = if (meshUiState.isInternetAvailable) "Online" else "Offline",
                            label = "İnternet",
                            valueColor = if (meshUiState.isInternetAvailable) YankiGreen else Color(0xFFFF6B6B),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}