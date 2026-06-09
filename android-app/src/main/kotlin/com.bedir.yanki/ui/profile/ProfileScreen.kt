package com.bedir.yanki.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bedir.yanki.ui.navigation.Screen
import com.bedir.yanki.ui.theme.YankiDarkBg
import com.bedir.yanki.ui.theme.YankiGreen
import com.bedir.yanki.ui.viewmodel.MeshViewModel

@Composable
fun ProfileScreen(navController: NavController, viewModel: MeshViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    fun navigateToWelcome() {
        navController.navigate(Screen.Welcome.route) {
            popUpTo(0) { inclusive = true }
        }
    }

    // --- SIFIRLAMA ONAYI DİALOGU ---
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Cihazı Sıfırla", fontWeight = FontWeight.Bold, color = Color.Red) },
            text = {
                Text("Tüm mesajlar, kişiler ve hesap bilgileri kalıcı olarak silinecek. Bu işlem geri alınamaz.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        viewModel.clearAllData { navigateToWelcome() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Evet, Sıfırla", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Üst Profil Kartı (Koyu)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(YankiDarkBg)
                .padding(top = 48.dp, bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (currentUser?.full_name ?: "??").split(" ").joinToString("") { it.take(1) }.uppercase(),
                            color = Color.White,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = YankiGreen,
                        modifier = Modifier
                            .size(32.dp)
                            .background(YankiDarkBg, CircleShape)
                            .padding(2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = currentUser?.full_name ?: "İsimsiz Kullanıcı",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "YANKI Kullanıcısı",
                    color = Color.Gray,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileBadge(
                        icon = Icons.Default.Favorite,
                        text = currentUser?.blood_type ?: "-",
                        color = Color(0xFFE74C3C).copy(alpha = 0.15f),
                        textColor = Color(0xFFE74C3C)
                    )
                    ProfileBadge(
                        icon = Icons.Default.Lock,
                        text = "Ed25519 · Key",
                        color = Color.White.copy(alpha = 0.1f),
                        textColor = Color.White
                    )
                }
            }
        }

        // Menü Öğeleri
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "SAĞLIK BİLGİLERİ",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            ProfileMenuItem(
                icon = Icons.Default.FavoriteBorder,
                title = "Sağlık bilgileri",
                subtitle = "Kan grubu, alerjiler, hastalıklar",
                iconBg = Color(0xFFE6B0AA),
                onClick = { navController.navigate(Screen.HealthDetail.route) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "UYGULAMA",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            ProfileMenuItem(
                icon = Icons.Default.Settings,
                title = "Ayarlar",
                subtitle = "Güvenlik, ağ, tercihler",
                iconBg = YankiDarkBg,
                onClick = { navController.navigate(Screen.Settings.route) }
            )

            ProfileMenuItem(
                icon = Icons.Default.Warning,
                title = "Cihazı sıfırla",
                subtitle = "Tüm veriler kalıcı olarak silinir",
                iconBg = Color(0xFFFADBD8),
                iconColor = Color.Red,
                titleColor = Color.Red,
                onClick = { showResetDialog = true }
            )
        }
    }
}

@Composable
fun ProfileBadge(icon: ImageVector, text: String, color: Color, textColor: Color) {
    Surface(
        color = color,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = textColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = text, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconBg: Color,
    iconColor: Color = Color.White,
    titleColor: Color = Color.Black,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, color = titleColor)
                Text(text = subtitle, color = Color.Gray, fontSize = 12.sp)
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.LightGray
            )
        }
    }
}
