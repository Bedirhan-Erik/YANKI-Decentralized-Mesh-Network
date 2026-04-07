package com.bedir.yanki.ui.main.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bedir.yanki.data.local.entity.UserEntity
import com.bedir.yanki.ui.theme.YankiCardBg
import com.bedir.yanki.ui.theme.YankiGreen

@Composable
fun NeighborMap(
    neighbors: List<UserEntity>,
    userLat: Double,
    userLon: Double
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = YankiCardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Komşu Haritası",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                
                Button(
                    onClick = {
                        val gmmIntentUri = Uri.parse("geo:$userLat,$userLon?z=15")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        context.startActivity(mapIntent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = YankiGreen.copy(alpha = 0.2f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = YankiGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Haritayı Aç", color = YankiGreen, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Basit bir liste şeklinde komşuların son görüldüğü yerleri veya mesafelerini gösterebiliriz
            // Gerçek MapView entegrasyonu (Google Maps SDK veya OSM) ek bağımlılık ve API Key gerektirir.
            // Bu yüzden şimdilik listeden haritaya yönlendiren bir yapı kuruyoruz.
            
            if (neighbors.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Yakında komşu bulunamadı", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                neighbors.forEach { neighbor ->
                    NeighborListItem(neighbor)
                }
            }
        }
    }
}

@Composable
fun NeighborListItem(neighbor: UserEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(YankiGreen.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = neighbor.username.take(1).uppercase(), color = YankiGreen, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(text = neighbor.username, color = Color.White, fontWeight = FontWeight.Medium)
            val timeDiff = System.currentTimeMillis() - neighbor.last_seen
            val timeText = when {
                timeDiff < 60000 -> "Az önce"
                timeDiff < 3600000 -> "${timeDiff / 60000} dk önce"
                else -> "Uzun süre önce"
            }
            Text(text = "Son görülme: $timeText", color = Color.Gray, fontSize = 12.sp)
        }
        
        // Komşu çevrimiçiyse yeşil nokta
        if (System.currentTimeMillis() - neighbor.last_seen < 60000) {
            Box(modifier = Modifier.size(8.dp).background(YankiGreen, CircleShape))
        }
    }
}
