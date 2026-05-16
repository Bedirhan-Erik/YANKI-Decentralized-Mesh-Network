package com.bedir.yanki.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.bedir.yanki.ui.theme.YankiCardBg
import com.bedir.yanki.ui.theme.YankiDarkBg
import com.bedir.yanki.ui.theme.YankiGreen
import com.bedir.yanki.ui.viewmodel.MeshViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHealthScreen(
    navController: NavController,
    viewModel: MeshViewModel,
    type: String
) {
    val currentUser by viewModel.currentUser.collectAsState()
    var value by remember { mutableStateOf("") }
    val bloodTypes = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "0+", "0-")

    LaunchedEffect(currentUser) {
        currentUser?.let {
            value = when (type) {
                "blood_type" -> it.blood_type ?: ""
                "allergies" -> it.allergies ?: ""
                "medications" -> it.medications ?: ""
                "emergency_contact" -> it.emergency_contact ?: ""
                else -> ""
            }
        }
    }

    val title = when (type) {
        "blood_type" -> "Kan Grubu"
        "allergies" -> "Alerjiler"
        "medications" -> "İlaçlar"
        "emergency_contact" -> "Acil İletişim"
        else -> "Düzenle"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
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
                .fillMaxSize()
        ) {
            if (type == "blood_type") {
                Text(
                    text = "Kan Grubunuzu Seçin",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(bloodTypes) { bloodType ->
                        val isSelected = value == bloodType
                        Box(
                            modifier = Modifier
                                .height(56.dp)
                                .background(
                                    if (isSelected) YankiGreen else Color.White,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { value = bloodType },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = bloodType,
                                color = if (isSelected) YankiDarkBg else Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(title) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            Button(
                onClick = {
                    viewModel.viewModelScope.launch {
                        when (type) {
                            "blood_type" -> viewModel.repository.updateUserHealth(bloodType = value)
                            "allergies" -> viewModel.repository.updateUserHealth(allergies = value)
                            "medications" -> viewModel.repository.updateUserHealth(medications = value)
                            "emergency_contact" -> viewModel.repository.updateUserHealth(emergencyContact = value)
                        }
                        navController.popBackStack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = YankiGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Kaydet", color = YankiDarkBg, fontWeight = FontWeight.Bold)
            }
        }
    }
}
