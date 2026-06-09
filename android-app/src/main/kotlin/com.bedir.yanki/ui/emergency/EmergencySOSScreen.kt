package com.bedir.yanki.ui.emergency

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bedir.yanki.data.local.entity.EmergencySignalEntity
import com.bedir.yanki.ui.navigation.Screen
import com.bedir.yanki.ui.theme.*
import com.bedir.yanki.ui.viewmodel.MeshViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EmergencySOSScreen(navController: NavController, viewModel: MeshViewModel) {
    var selectedDisaster by remember { mutableStateOf("Deprem") }
    val snackbarHostState = remember { SnackbarHostState() }
    val meshStatus by viewModel.meshStatus.collectAsState()
    val allSosSignals by viewModel.allSosSignals.collectAsState()
    val mySosSignals by viewModel.mySosSignals.collectAsState()
    val userStatus by viewModel.userStatus.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.sosEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color(0xFF0D1219)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // --- HEADER ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { navController.navigate(Screen.Home.route) }) {
                            Icon(Icons.Default.Home, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Acil Sinyal",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val statusColor = if (meshStatus.isMeshActive) YankiGreen else Color.Gray
                        Box(modifier = Modifier.size(8.dp).background(statusColor, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (meshStatus.isMeshActive) "Ağ Aktif" else "Çevrimdışı",
                            color = statusColor,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // --- SOS BUTTON ---
            item {
                PulsingSosButton(
                    isActive = mySosSignals.isNotEmpty(),
                    neighborCount = meshStatus.neighborCount,
                    onClick = {
                        viewModel.sendEmergencySOS(
                            selectedDisaster,
                            userStatus.latitude,
                            userStatus.longitude,
                            userStatus.batteryLevel
                        )
                    }
                )
            }

            // --- DISASTER TYPE CHIPS ---
            item {
                Text(text = "Afet türü", color = Color(0xFF8A95A3), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DisasterChip("Deprem", "🌋", selectedDisaster == "Deprem") { selectedDisaster = "Deprem" }
                    DisasterChip("Sel", "🌧️", selectedDisaster == "Sel") { selectedDisaster = "Sel" }
                    DisasterChip("Yangın", "🔥", selectedDisaster == "Yangın") { selectedDisaster = "Yangın" }
                    DisasterChip("Diğer", "🕒", selectedDisaster == "Diğer") { selectedDisaster = "Diğer" }
                }
            }

            // --- STATUS BOXES ---
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatusBox(
                        label = "Batarya",
                        value = "%${userStatus.batteryLevel}",
                        modifier = Modifier.weight(1f),
                        color = if (userStatus.batteryLevel > 20) YankiGreen else Color.Red
                    )
                    StatusBox(
                        label = "Komşu",
                        value = "${meshStatus.neighborCount}",
                        modifier = Modifier.weight(1f),
                        color = if (meshStatus.neighborCount > 0) YankiGreen else Color.Gray
                    )
                    StatusBox(
                        label = "İnternet",
                        value = if (meshStatus.isInternetAvailable) "Bağlı" else "Yok",
                        modifier = Modifier.weight(1f),
                        color = if (meshStatus.isInternetAvailable) YankiGreen else Color(0xFFE67E22)
                    )
                }
            }

            // --- MY ACTIVE SIGNALS ---
            if (mySosSignals.isNotEmpty()) {
                item {
                    Text(
                        text = "Aktif Sinyallerim",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
                items(mySosSignals.take(3)) { signal ->
                    MySignalCard(signal = signal, neighborCount = meshStatus.neighborCount)
                }
            }

            // --- RECEIVED SOS SIGNALS FROM MESH ---
            val receivedSignals = allSosSignals.filter { it.user_id != viewModel.repository.currentUserId }
            if (receivedSignals.isNotEmpty()) {
                item {
                    Text(
                        text = "Ağdan Gelen Sinyaller",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
                items(receivedSignals.take(5)) { signal ->
                    ReceivedSignalCard(signal = signal)
                }
            }
        }
    }
}

@Composable
fun PulsingSosButton(isActive: Boolean, neighborCount: Int, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "sos_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sos_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(if (isActive) Color(0xFF1A0A0A) else Color(0xFF1A0808))
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier
                    .size(140.dp)
                    .scale(scale)
                    .clickable { onClick() },
                shape = CircleShape,
                color = Color(0xFFFF3333),
                shadowElevation = if (isActive) 16.dp else 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "SOS",
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = if (isActive) "SOS Yayında" else "SOS Yayınla",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) Color(0xFFFF5252) else Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (neighborCount > 0)
                    "$neighborCount komşu üzerinden ağa yayılıyor"
                else
                    "Yakında komşu bulunamadı",
                color = Color(0xFF8A95A3),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MySignalCard(signal: EmergencySignalEntity, neighborCount: Int) {
    val hopColor = when {
        signal.hop_count == 0 -> YankiGreen
        signal.hop_count <= 2 -> Color(0xFFE67E22)
        else -> Color(0xFFE74C3C)
    }
    val hopLabel = when (signal.hop_count) {
        0 -> "Direkt bağlantı"
        1 -> "1 cihazdan sekti"
        else -> "${signal.hop_count} cihazdan sekti"
    }
    val timeAgo = remember(signal.timestamp) {
        val diff = System.currentTimeMillis() - signal.timestamp
        when {
            diff < 60_000L -> "Az önce"
            diff < 3_600_000L -> "${diff / 60_000} dk önce"
            else -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(signal.timestamp))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1A0A0A))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emergencyIcon(signal.emergency_type), fontSize = 28.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = signal.emergency_type,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).background(hopColor, CircleShape))
                Spacer(modifier = Modifier.width(5.dp))
                Text(text = hopLabel, color = hopColor, fontSize = 12.sp)
                Text(text = " · ", color = Color(0xFF4A5568), fontSize = 12.sp)
                Text(text = timeAgo, color = Color(0xFF8A95A3), fontSize = 12.sp)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${signal.hop_count}",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = hopColor
            )
            Text(text = "hop", color = Color(0xFF4A5568), fontSize = 11.sp)
        }
    }
}

@Composable
fun ReceivedSignalCard(signal: EmergencySignalEntity) {
    val hopColor = when {
        signal.hop_count <= 1 -> YankiGreen
        signal.hop_count <= 3 -> Color(0xFFE67E22)
        else -> Color(0xFFE74C3C)
    }
    val timeAgo = remember(signal.timestamp) {
        val diff = System.currentTimeMillis() - signal.timestamp
        when {
            diff < 60_000L -> "Az önce"
            diff < 3_600_000L -> "${diff / 60_000} dk önce"
            else -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(signal.timestamp))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(YankiCardBg)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emergencyIcon(signal.emergency_type), fontSize = 24.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = signal.user_name ?: "Bilinmiyor",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                text = signal.emergency_type,
                color = Color(0xFF8A95A3),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = timeAgo, color = Color(0xFF4A5568), fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "📡", fontSize = 12.sp)
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${signal.hop_count} hop",
                    color = hopColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = when (signal.hop_count) {
                    1 -> "1 cihazdan"
                    else -> "${signal.hop_count} cihazdan"
                },
                color = Color(0xFF4A5568),
                fontSize = 10.sp
            )
        }
    }
}

private fun emergencyIcon(type: String) = when (type.lowercase()) {
    "deprem" -> "🌋"
    "sel" -> "🌧️"
    "yangın", "yangin" -> "🔥"
    else -> "🆘"
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
                .background(if (isSelected) Color(0xFF2C1A1A) else YankiCardBg),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 24.sp)
        }
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isSelected) Color(0xFFFF5252) else Color(0xFF8A95A3)
        )
    }
}

@Composable
fun StatusBox(label: String, value: String, modifier: Modifier, color: Color) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(YankiCardBg)
            .padding(14.dp)
    ) {
        Column {
            Text(text = label, color = Color(0xFF8A95A3), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
