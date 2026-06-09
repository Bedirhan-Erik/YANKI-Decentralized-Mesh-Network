package com.bedir.yanki.ui.registration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.bedir.yanki.ui.navigation.Screen
import com.bedir.yanki.ui.theme.YankiCardBg
import com.bedir.yanki.ui.theme.YankiDarkBg
import com.bedir.yanki.ui.theme.YankiGreen
import com.bedir.yanki.ui.viewmodel.registration.RegistrationViewModel

private val NAME_MAX = 50
private val VALID_NAME_CHARS = Regex("[a-zA-ZğüşıöçĞÜŞİÖÇ ]*")

@Composable
fun RegisterStep1Screen(
    navController: NavController,
    viewModel: RegistrationViewModel = hiltViewModel()
) {
    val nameError = viewModel.fullName.isNotBlank() &&
            (viewModel.fullName.trim().isEmpty() || !viewModel.fullName.matches(VALID_NAME_CHARS))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(YankiDarkBg)
            .padding(24.dp)
    ) {
        Text(
            text = "Adım 1/3",
            color = YankiGreen,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Kimlik Bilgileri",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = viewModel.fullName,
            onValueChange = { input ->
                if (input.length <= NAME_MAX && input.matches(VALID_NAME_CHARS)) {
                    viewModel.fullName = input
                }
            },
            label = { Text("Ad Soyad") },
            placeholder = { Text("Örn: Ahmet Yılmaz", color = Color.Gray) },
            singleLine = true,
            isError = nameError,
            supportingText = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (nameError) {
                        Text("Yalnızca harf ve boşluk girilebilir", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    Text(
                        text = "${viewModel.fullName.length}/$NAME_MAX",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedTextColor = Color.White,
                focusedTextColor = Color.White,
                unfocusedBorderColor = Color.Gray,
                focusedBorderColor = YankiGreen,
                unfocusedLabelColor = Color.Gray,
                focusedLabelColor = YankiGreen,
                errorBorderColor = MaterialTheme.colorScheme.error,
                errorLabelColor = MaterialTheme.colorScheme.error
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = YankiCardBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = YankiGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Uçtan Uca Şifreleme",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Bilgileriniz cihazınızda üretilen anahtarla şifrelenir. Biz bile göremeyiz.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { navController.navigate(Screen.RegisterStep2.route) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = YankiGreen),
            enabled = viewModel.fullName.trim().length >= 2 && !nameError
        ) {
            Text(text = "Devam Et", color = YankiDarkBg, fontWeight = FontWeight.Bold)
        }
    }
}
