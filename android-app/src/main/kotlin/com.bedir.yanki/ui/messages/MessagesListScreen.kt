package com.bedir.yanki.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
fun MessagesListScreen(viewModel: MeshViewModel, navController: NavController) {
    val uiState by viewModel.meshStatus.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 24.dp)
    ) {
        // --- ÜST BAŞLIK VE İKON ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "${uiState.neighborCount} Kişi çevrimiçi", color = Color.Gray, fontSize = 14.sp)
                Text(text = "Mesajlar", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold))
            }
            IconButton(
                onClick = { /* Yeni Mesaj */ },
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(YankiDarkBg)
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- ÇEVRİMİÇİ KOMŞULAR BARI (LazyRow) ---
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(uiState.neighbors) { neighbor ->
                OnlineNeighborAvatar(name = neighbor.username)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- TÜM AĞA YAYIN BANNERI ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFF0F0))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(Color.Red, CircleShape))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Tüm ağa yayın aktif", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
        

        // --- SOHBET LİSTESİ (LazyColumn) ---
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp)
        ) {
            items(uiState.neighbors) { neighbor ->
                ChatItem(
                    name = neighbor.username,
                    lastMsg = "Mesajlaşmaya başlayın...",
                    time = "--:--",
                    unreadCount = 0,
                    color = Color(0xFFF0F0F0),
                    onClick = {
                        navController.navigate(Screen.ChatDetail.createRoute(neighbor.user_id, neighbor.username))
                    }
                )
            }
            
            if (uiState.neighbors.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Yakınlarda kimse yok...", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun OnlineNeighborAvatar(name: String) {
    val initials = name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFF0F0F0)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = initials, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = YankiDarkBg)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = name.split(" ")[0], fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun ChatItem(name: String, lastMsg: String, time: String, unreadCount: Int, color: Color, onClick: () -> Unit) {
    val initials = name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape).background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(text = initials, fontWeight = FontWeight.Bold, color = Color.Black.copy(alpha = 0.6f))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = lastMsg, color = Color.Gray, fontSize = 14.sp, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = time, color = Color.LightGray, fontSize = 12.sp)
            if (unreadCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = unreadCount.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
