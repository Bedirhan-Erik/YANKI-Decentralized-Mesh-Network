package com.bedir.yanki.ui.main

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bedir.yanki.ui.navigation.Screen
import com.bedir.yanki.ui.theme.*
import com.bedir.yanki.ui.viewmodel.MeshViewModel
import com.bedir.yanki.ui.radar.RadarView
import com.bedir.yanki.ui.main.components.StatItem
import com.bedir.yanki.ui.main.components.NeighborMap

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun MainDashboard(navController: NavController, viewModel: MeshViewModel = hiltViewModel()) {
    val meshUiState by viewModel.meshStatus.collectAsState()
    val userStatus by viewModel.userStatus.collectAsState()
    val messages by viewModel.allMessages.collectAsState()
    val context = navController.context
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    // Dashboard açıldığında mesh servisini otomatik başlat
    LaunchedEffect(Unit) {
        viewModel.startMeshService()
    }

    val lastMessage = messages.lastOrNull()
    val msgStatusText = if (messages.isEmpty()) "Sohbet yok" else "Aktif"
    val msgSubTitle = if (lastMessage != null) {
        val content = try { String(lastMessage.content_blob) } catch(e: Exception) { "Mesaj..." }
        val displayContent = if (content.length > 12) content.take(12) + "..." else content
        displayContent
    } else {
        "Sohbet başlatın"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(YankiDarkBg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            // --- RADAR KARTI ---
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = YankiCardBg),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Text(
                                text = if (meshUiState.isMeshActive) "Canlı" else "Çevrimdışı",
                                color = if (meshUiState.isMeshActive) Color.Red else YankiGreyDot,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        // Radar boyutunu ekranın küçük bir kısmına sığacak şekilde ayarla
                        val radarSize = if (screenHeight < 600.dp) 160.dp else 220.dp
                        RadarView(
                            isMeshActive = meshUiState.isMeshActive,
                            neighborCount = meshUiState.neighborCount,
                            neighbors = meshUiState.neighborPoints,
                            modifier = Modifier.size(radarSize).align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatItem(value = "${meshUiState.neighborCount}", label = "Cihaz", valueColor = YankiGreen, modifier = Modifier.weight(1f))
                            // VerticalDivider yerine özel bir ayırıcı kullanıyoruz (BOM sürümü uyumluluğu için)
                            Box(modifier = Modifier.height(30.dp).width(1.dp).background(Color.Gray.copy(alpha = 0.3f)))
                            StatItem(value = meshUiState.coverageRange, label = "Kapsama", modifier = Modifier.weight(1f))
                            Box(modifier = Modifier.height(30.dp).width(1.dp).background(Color.Gray.copy(alpha = 0.3f)))
                            StatItem(
                                value = if (meshUiState.isInternetAvailable) "On" else "Off",
                                label = "Web",
                                valueColor = if (meshUiState.isInternetAvailable) YankiGreen else Color(0xFFFF6B6B),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // --- ACİL SİNYAL GÖNDER KARTI ---
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .clickable { navController.navigate(Screen.Network.route) },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFFF5252)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        
                        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                            Text(text = "Acil Durum", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = "Mesh ağına yayınla", color = Color(0xFFD32F2F).copy(alpha = 0.6f), fontSize = 12.sp)
                        }

                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFFFF5252))
                    }
                }
            }

            // --- HIZLI ERİŞİM BAŞLIĞI ---
            item {
                Text(
                    text = "Hızlı erişim",
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, top = 24.dp),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            // --- KOMŞU HARİTASI ---
            item {
                NeighborMap(
                    neighbors = meshUiState.neighbors,
                    userLat = userStatus.latitude,
                    userLon = userStatus.longitude
                )
            }

            // --- HIZLI ERİŞİM KARTLARI ---
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickAccessCard(
                        title = "Harita",
                        statusText = if (meshUiState.isMeshActive) "Aktif" else "Kapalı",
                        subTitle = "${meshUiState.neighborCount} Komşu",
                        icon = Icons.Default.LocationOn,
                        statusColor = if (meshUiState.isMeshActive) YankiGreen else YankiGreyDot,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val gmmIntentUri = Uri.parse("geo:${userStatus.latitude},${userStatus.longitude}?z=15")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            try {
                                context.startActivity(mapIntent)
                            } catch (e: Exception) {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/@${userStatus.latitude},${userStatus.longitude},15z"))
                                context.startActivity(browserIntent)
                            }
                        }
                    )
                    QuickAccessCard(
                        title = "Mesajlar",
                        statusText = msgStatusText,
                        subTitle = msgSubTitle,
                        icon = Icons.Default.Email,
                        statusColor = if (messages.isEmpty()) YankiGreyDot else YankiGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.Messages.route) }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickAccessCard(
    modifier: Modifier = Modifier,
    title: String,
    statusText: String,
    subTitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    statusColor: Color = YankiGreen,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(140.dp).clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = YankiCardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(YankiDarkBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column {
                Text(text = title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                Text(text = subTitle, color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}
