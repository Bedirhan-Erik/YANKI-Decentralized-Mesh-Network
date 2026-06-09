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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bedir.yanki.data.local.entity.UserEntity
import com.bedir.yanki.ui.navigation.Screen
import com.bedir.yanki.ui.theme.*
import com.bedir.yanki.ui.viewmodel.MeshViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Consistent avatar color from user name
private fun avatarColor(name: String): Color {
    val colors = listOf(
        Color(0xFF2ECC71), Color(0xFF3498DB), Color(0xFF9B59B6),
        Color(0xFFE67E22), Color(0xFF1ABC9C), Color(0xFFE74C3C)
    )
    return colors[(name.hashCode() and 0x7FFFFFFF) % colors.size]
}

private fun formatTime(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000L -> "Az önce"
        diff < 3_600_000L -> "${diff / 60_000} dk"
        diff < 86_400_000L -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
    }
}

@Composable
fun MessagesListScreen(viewModel: MeshViewModel, navController: NavController) {
    val uiState by viewModel.meshStatus.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(YankiDarkBg, Color(0xFF0D1219))
                )
            )
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.navigate(Screen.Home.route) }) {
                    Icon(Icons.Default.Home, contentDescription = "Ana Sayfa", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "${uiState.neighborCount} kişi yakınlıkta",
                        color = YankiGreyDot,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Mesajlar",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(YankiCardBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White)
            }
        }

        // --- ONLINE NEIGHBORS ROW ---
        if (uiState.neighbors.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.neighbors) { neighbor ->
                    OnlineNeighborAvatar(name = neighbor.full_name ?: neighbor.username)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // --- BROADCAST BANNER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1E1A0E))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(8.dp).background(YankiGreen, CircleShape))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Mesh ağı aktif · Şifrelenmiş iletişim",
                color = YankiGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- CHAT LIST ---
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(uiState.neighbors) { neighbor ->
                ChatListItem(
                    neighbor = neighbor,
                    viewModel = viewModel,
                    onClick = {
                        navController.navigate(
                            Screen.ChatDetail.createRoute(
                                neighbor.user_id,
                                neighbor.full_name ?: neighbor.username
                            )
                        )
                    }
                )
            }

            if (uiState.neighbors.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "📡", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Yakında kimse yok",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Bluetooth çevrenizde YANKI kullanan\nbiri olduğunda burada görünür.",
                                color = YankiGreyDot,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatListItem(neighbor: UserEntity, viewModel: MeshViewModel, onClick: () -> Unit) {
    val lastMessage by viewModel.getLastMessageWithUser(neighbor.user_id).collectAsState(initial = null)
    val name = neighbor.full_name ?: neighbor.username
    val initials = name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase()
    val color = avatarColor(name)

    val lastMsgText = lastMessage?.let {
        try { String(it.content_blob) } catch (e: Exception) { "" }
    } ?: ""
    val lastMsgTime = lastMessage?.timestamp ?: 0L

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = if (lastMsgText.isNotBlank()) lastMsgText else "Mesajlaşmaya başlayın...",
                color = if (lastMsgText.isNotBlank()) Color(0xFF8A95A3) else Color(0xFF4A5568),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            if (lastMsgTime > 0L) {
                Text(
                    text = formatTime(lastMsgTime),
                    color = YankiGreyDot,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(YankiGreen, CircleShape)
            )
        }
    }

    Divider(
        modifier = Modifier.padding(start = 72.dp),
        color = Color(0xFF1B232E),
        thickness = 1.dp
    )
}

@Composable
fun OnlineNeighborAvatar(name: String) {
    val initials = name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase()
    val color = avatarColor(name)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = initials, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(YankiGreen, CircleShape)
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = name.split(" ")[0], fontSize = 11.sp, color = Color.White)
    }
}
