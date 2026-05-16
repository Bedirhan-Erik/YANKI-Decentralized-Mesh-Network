package com.bedir.yanki.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthDetailScreen(navController: NavController, viewModel: MeshViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sağlık Bilgileri", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8F9FA))
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            HealthSection(title = "KİMLİK") {
                HealthItem(
                    icon = Icons.Default.Favorite,
                    iconTint = Color(0xFFE74C3C),
                    title = "Kan grubu",
                    value = currentUser?.blood_type ?: "Seçilmedi",
                    valueColor = Color(0xFFE74C3C),
                    onClick = { navController.navigate(Screen.EditHealth.createRoute("blood_type")) }
                )
                HealthItem(
                    icon = Icons.Default.Person,
                    iconTint = Color(0xFF3498DB),
                    title = "Ad Soyad",
                    value = currentUser?.full_name ?: "Belirtilmedi",
                    showArrow = false
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            HealthSection(title = "TIBBİ BİLGİLER") {
                HealthItem(
                    icon = Icons.Default.Add,
                    iconTint = Color(0xFF2ECC71),
                    title = "Kullandığı ilaçlar",
                    value = currentUser?.medications ?: "Belirtilmedi",
                    onClick = { navController.navigate(Screen.EditHealth.createRoute("medications")) }
                )
                HealthItem(
                    icon = Icons.Default.Info,
                    iconTint = Color(0xFF3498DB),
                    title = "Alerjiler",
                    value = currentUser?.allergies ?: "Belirtilmedi",
                    onClick = { navController.navigate(Screen.EditHealth.createRoute("allergies")) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            HealthSection(title = "ACİL İLETİŞİM") {
                HealthItem(
                    icon = Icons.Default.Person,
                    iconTint = Color(0xFFE91E63),
                    title = "Acil durum yakını",
                    value = currentUser?.emergency_contact ?: "Eklenmedi",
                    onClick = { navController.navigate(Screen.EditHealth.createRoute("emergency_contact")) }
                )
            }
        }
    }
}

@Composable
fun HealthSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            color = Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun HealthItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    subtitle: String? = null,
    valueColor: Color = Color.Black,
    showArrow: Boolean = true,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = showArrow) { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconTint.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, fontWeight = FontWeight.Bold, color = Color.Black)
                if (value.isNotEmpty()) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = value, color = valueColor, fontWeight = FontWeight.Medium)
                }
            }
            if (subtitle != null) {
                Text(text = subtitle, color = Color.Gray, fontSize = 12.sp)
            }
        }

        if (showArrow) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.LightGray
            )
        }
    }
}
