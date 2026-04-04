package com.bedir.yanki.ui.emergency

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.bedir.yanki.ui.theme.*
import com.bedir.yanki.ui.viewmodel.MeshViewModel

@Composable
fun EmergencySOSScreen(viewModel: MeshViewModel) {
    var selectedDisaster by remember { mutableStateOf("Deprem") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // Tasarımdaki açık renk arka plan
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- ÜST BAŞLIK ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Acil Sinyal",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(Color.Red, CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Canlı", color = Color.Red, fontWeight = FontWeight.Medium)
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
                            // TODO: ViewModel üzerinden SOS fırlat!
                            viewModel.sendEmergencySOS(selectedDisaster, 38.4192, 21.1287, 56)
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatusBox(label = "Batarya", value = "%56", modifier = Modifier.weight(1f), color = YankiGreen)
            StatusBox(label = "Konum", value = "38.4192", subValue = "21.1287 Aktif", modifier = Modifier.weight(1f), color = Color.Blue)
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