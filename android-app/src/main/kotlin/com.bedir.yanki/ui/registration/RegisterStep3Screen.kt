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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bedir.yanki.ui.navigation.Screen
import com.bedir.yanki.ui.theme.YankiDarkBg
import com.bedir.yanki.ui.theme.YankiGreen
import com.bedir.yanki.ui.viewmodel.registration.RegistrationViewModel
import kotlinx.coroutines.launch

private const val PHONE_MAX = 12

private fun filterPhone(input: String): String {
    if (input.isEmpty()) return input
    return if (input.startsWith("+")) {
        "+" + input.drop(1).filter { it.isDigit() }
    } else {
        input.filter { it.isDigit() }
    }.take(PHONE_MAX)
}

private fun isValidPhone(phone: String): Boolean {
    val digits = phone.replace("+", "")
    return digits.length >= 10 && digits.all { it.isDigit() }
}

@Composable
fun RegisterStep3Screen(
    navController: NavController,
    viewModel: RegistrationViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val phoneError = viewModel.emergencyContact.isNotBlank() && !isValidPhone(viewModel.emergencyContact)

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
            value = viewModel.emergencyContact,
            onValueChange = { viewModel.emergencyContact = filterPhone(it) },
            label = { Text("Acil Durum Yakını (Telefon)") },
            placeholder = { Text("+90 5XX XXX XX XX", color = Color.Gray) },
            singleLine = true,
            isError = phoneError,
            supportingText = {
                if (phoneError) {
                    Text(
                        "Geçerli bir telefon numarası girin (en az 10 rakam)",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp
                    )
                } else {
                    Text(
                        "${viewModel.emergencyContact.replace("+", "").length} rakam",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Default.Call, contentDescription = null, tint = YankiGreen)
            },
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

        Text(
            text = "Kaydı tamamladığınızda cihazınız için özel bir anahtar çifti oluşturulacak ve profiliniz yerel ağda yayınlanmaya hazır hale gelecektir.",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, YankiGreen),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(text = "Geri", color = YankiGreen)
            }

            Button(
                onClick = {
                    scope.launch {
                        viewModel.completeRegistration()
                        navController.navigate(Screen.RegisterSuccess.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                },
                modifier = Modifier
                    .weight(2f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = YankiGreen),
                enabled = isValidPhone(viewModel.emergencyContact)
            ) {
                Text(text = "Kaydı Tamamla", color = YankiDarkBg, fontWeight = FontWeight.Bold)
            }
        }
    }
}
