package com.bedir.yanki.ui.registration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bedir.yanki.data.local.entity.UserEntity
import com.bedir.yanki.ui.navigation.Screen
import com.bedir.yanki.ui.theme.YankiDarkBg
import com.bedir.yanki.ui.theme.YankiGreen
import com.bedir.yanki.ui.viewmodel.MeshViewModel
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun RegisterStep3Screen(
    navController: NavController,
    fullName: String,
    username: String,
    bloodType: String,
    allergies: String,
    medications: String,
    viewModel: MeshViewModel = hiltViewModel()
) {
    var emergencyContact by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(YankiDarkBg)
            .padding(24.dp)
    ) {
        Text(
            text = "Adım 3/3",
            color = YankiGreen,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Son Dokunuş",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = emergencyContact,
            onValueChange = { emergencyContact = it },
            label = { Text("Acil Durum Yakını (Telefon)") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Call, contentDescription = null, tint = YankiGreen) },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedTextColor = Color.White,
                focusedTextColor = Color.White,
                unfocusedBorderColor = Color.Gray,
                focusedBorderColor = YankiGreen,
                unfocusedLabelColor = Color.Gray,
                focusedLabelColor = YankiGreen
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Kaydı tamamladığınızda cihazınız için özel bir anahtar çifti oluşturulacak ve profiliniz yerel ağda yayınlanmaya hazır hale gelecektir.",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                scope.launch {
                    val newUser = UserEntity(
                        user_id = UUID.randomUUID().toString(),
                        username = username,
                        full_name = fullName,
                        public_key = byteArrayOf(), // Repository içinde otomatik oluşturuluyor
                        last_seen = System.currentTimeMillis(),
                        is_trusted = true,
                        blood_type = bloodType,
                        allergies = allergies,
                        medications = medications,
                        emergency_contact = emergencyContact
                    )
                    viewModel.repository.saveUser(newUser)
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = YankiGreen),
            enabled = emergencyContact.length >= 10
        ) {
            Text(text = "Kaydı Tamamla", color = YankiDarkBg, fontWeight = FontWeight.Bold)
        }
    }
}
