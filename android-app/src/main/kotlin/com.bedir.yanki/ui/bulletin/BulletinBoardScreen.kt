package com.bedir.yanki.ui.bulletin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bedir.yanki.data.local.entity.BulletinEntity
import com.bedir.yanki.ui.viewmodel.MeshViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulletinBoardScreen(viewModel: MeshViewModel) {
    val bulletins by viewModel.allBulletins.collectAsState()
    var showPostDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredBulletins = remember(bulletins, selectedFilter) {
        if (selectedFilter == "ALL") bulletins else bulletins.filter { it.type == selectedFilter }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yerel Duyuru Panosu") },
                actions = {
                    FilterMenu(selectedFilter) { selectedFilter = it }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showPostDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Duyuru Paylaş")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (filteredBulletins.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Henüz duyuru yok", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredBulletins.sortedByDescending { it.timestamp }) { bulletin ->
                        BulletinItem(bulletin)
                    }
                }
            }
        }

        if (showPostDialog) {
            PostBulletinDialog(
                onDismiss = { showPostDialog = false },
                onPost = { content, type ->
                    viewModel.postBulletin(content, type)
                    showPostDialog = false
                }
            )
        }
    }
}

@Composable
fun FilterMenu(selectedFilter: String, onFilterSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.FilterList, contentDescription = "Filtrele")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Hepsi") },
                onClick = { onFilterSelected("ALL"); expanded = false }
            )
            DropdownMenuItem(
                text = { Text("Bilgi (INFO)") },
                onClick = { onFilterSelected("INFO"); expanded = false }
            )
            DropdownMenuItem(
                text = { Text("İhtiyaç (NEED)") },
                onClick = { onFilterSelected("NEED"); expanded = false }
            )
            DropdownMenuItem(
                text = { Text("Uyarı (ALERT)") },
                onClick = { onFilterSelected("ALERT"); expanded = false }
            )
        }
    }
}

@Composable
fun BulletinItem(bulletin: BulletinEntity) {
    val cardColor = when (bulletin.type) {
        "ALERT" -> Color(0xFFFFEBEE)
        "NEED" -> Color(0xFFE3F2FD)
        else -> Color.White
    }

    val typeLabel = when (bulletin.type) {
        "ALERT" -> "⚠️ UYARI"
        "NEED" -> "🔍 İHTİYAÇ"
        else -> "ℹ️ BİLGİ"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bulletin.sender_name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = typeLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (bulletin.type == "ALERT") Color.Red else Color.Blue
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = bulletin.content)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTimestamp(bulletin.timestamp),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Menzil: ${bulletin.ttl} atlama",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostBulletinDialog(onDismiss: () -> Unit, onPost: (String, String) -> Unit) {
    var content by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("INFO") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Duyuru Paylaş") },
        text = {
            Column {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Mesajınız") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Duyuru Tipi:", fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedType == "INFO", onClick = { selectedType = "INFO" })
                    Text("Bilgi")
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(selected = selectedType == "NEED", onClick = { selectedType = "NEED" })
                    Text("İhtiyaç")
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(selected = selectedType == "ALERT", onClick = { selectedType = "ALERT" })
                    Text("Uyarı")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (content.isNotBlank()) onPost(content, selectedType) },
                enabled = content.isNotBlank()
            ) {
                Text("Paylaş")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm, dd.MM.yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
