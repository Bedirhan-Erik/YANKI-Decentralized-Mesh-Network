package com.bedir.yanki.ui.emergency

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bedir.yanki.ui.navigation.Screen
import com.bedir.yanki.ui.theme.*
import com.bedir.yanki.ui.viewmodel.MeshViewModel

@Composable
fun EmergencySOSScreen(navController: NavController, viewModel: MeshViewModel) {
    var selectedDisaster by remember { mutableStateOf("Deprem") }
    val snackbarHostState = remember { SnackbarHostState() }
    val meshStatus by viewModel.meshStatus.collectAsState()

    // SOS mesajlarını dinle
    LaunchedEffect(Unit) {
        viewModel.sosEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- ÜST BAŞLIK ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.navigate(Screen.Home.route) }) {
                        Icon(Icons.Default.Home, contentDescription = "Ana Sayfa", tint = Color.Black)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Acil Sinyal",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = if (meshStatus.isMeshActive) Color.Red else Color.Gray
                    val statusText = if (meshStatus.isMeshActive) "Canlı" else "Çevrimdışı"
                    
                    Box(modifier = Modifier.size(10.dp).background(statusColor, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = statusText, color = statusColor, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // --- MERKEZİ SOS BUTONU KARTI ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFFFFF0F0)) // Hafif kırmızımsı arka plan
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Büyük Kırmızı SOS Butonu
                    Surface(
                        modifier = Modifier
                            .size(140.dp)
                            .clickable {
                                val status = viewModel.userStatus.value
                                viewModel.sendEmergencySOS(
                                    selectedDisaster,
                                    status.latitude,
                                    status.longitude,
                                    status.batteryLevel
                                )
                            },
                        shape = CircleShape,
                        color = Color(0xFFFF5252),
                        shadowElevation = 8.dp
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "SOS",
                            tint = Color.White,
                            modifier = Modifier.padding(32.dp).size(60.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = "SOS Yayınla", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF421C1C))
                    Text(
                        text = "Tüm mesh ağına konumunu ve\ndurumunu iletir",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = Color.Red.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- AFET TÜRÜ SEÇİMİ ---
            Text(text = "Afet türü", modifier = Modifier.align(Alignment.Start), color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DisasterChip("Deprem", "🌋", selectedDisaster == "Deprem") { selectedDisaster = "Deprem" }
                DisasterChip("Sel", "🌧️", selectedDisaster == "Sel") { selectedDisaster = "Sel" }
                DisasterChip("Yangın", "🔥", selectedDisaster == "Yangın") { selectedDisaster = "Yangın" }
                DisasterChip("Diğer", "🕒", selectedDisaster == "Diğer") { selectedDisaster = "Diğer" }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- DURUM BİLGİLERİ (Batarya ve Konum) ---
            val userStatus by viewModel.userStatus.collectAsState()

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatusBox(
                    label = "Batarya",
                    value = "%${userStatus.batteryLevel}",
                    modifier = Modifier.weight(1f),
                    color = if (userStatus.batteryLevel > 20) YankiGreen else Color.Red
                )
                StatusBox(
                    label = "Konum",
                    value = String.format(java.util.Locale.getDefault(), "%.4f", userStatus.latitude),
                    subValue = String.format(java.util.Locale.getDefault(), "%.4f Aktif", userStatus.longitude),
                    modifier = Modifier.weight(1f),
                    color = Color.Blue
                )
            }
        }
    }
}

@Composable
fun DisasterChip(label: String, icon: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isSelected) Color(0xFFFFEAEA) else Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 24.sp)
        }
        Text(text = label, fontSize = 12.sp, color = if (isSelected) Color.Black else Color.Gray)
    }
}

@Composable
fun StatusBox(label: String, value: String, subValue: String? = null, modifier: Modifier, color: Color) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF8F9FA))
            .padding(16.dp)
    ) {
        Column {
            Text(text = label, color = Color.Gray, fontSize = 12.sp)
            Text(text = value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (subValue != null) Text(text = subValue, color = Color.Gray, fontSize = 10.sp)
        }
    }
}
