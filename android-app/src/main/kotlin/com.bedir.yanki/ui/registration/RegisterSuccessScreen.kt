package com.bedir.yanki.ui.registration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bedir.yanki.ui.navigation.Screen
import com.bedir.yanki.ui.theme.YankiCardBg
import com.bedir.yanki.ui.theme.YankiDarkBg
import com.bedir.yanki.ui.theme.YankiGreen
import com.bedir.yanki.ui.viewmodel.registration.RegistrationViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RegisterSuccessScreen(
    navController: NavController,
    viewModel: RegistrationViewModel = hiltViewModel()
) {
    val sdf = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale("tr"))
    val dateString = sdf.format(Date(viewModel.registrationTimestamp))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(YankiDarkBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Başarı İkonu (Radar stilinde halkalı)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(120.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(YankiGreen.copy(alpha = 0.1f))
            )
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(YankiGreen.copy(alpha = 0.2f))
            )
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(YankiGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = YankiDarkBg,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Hesabın hazır!",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Ed25519 anahtar çiftin oluşturuldu.\nMesh ağına bağlanmaya hazırsın.",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Bilgi Kartı
        Card(
            colors = CardDefaults.cardColors(containerColor = YankiCardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "PUBLIC KEY",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ed25519 · ${viewModel.generatedPublicKey.take(8)}...${viewModel.generatedPublicKey.takeLast(4)}",
                    color = YankiGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Divider(
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                Text(
                    text = "KAYIT ZAMANI",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateString,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(64.dp))

        Button(
            onClick = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Welcome.route) { inclusive = true }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = YankiGreen),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(
                text = "Uygulamaya gir",
                color = YankiDarkBg,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Sayfa göstergesi
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
            Box(modifier = Modifier.width(24.dp).height(8.dp).clip(CircleShape).background(YankiGreen))
        }
    }
}
