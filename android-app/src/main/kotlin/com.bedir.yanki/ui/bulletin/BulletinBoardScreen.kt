package com.bedir.yanki.ui.bulletin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Announcement
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
import com.bedir.yanki.ui.theme.*
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
            CenterAlignedTopAppBar(
                title = { Text("Duyuru Panosu", color = Color.White, fontWeight = FontWeight.Bold) },
                actions = {
                    FilterMenu(selectedFilter) { selectedFilter = it }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = YankiDarkBg
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showPostDialog = true },
                containerColor = YankiGreen,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Duyuru Paylaş")
            }
        },
        containerColor = YankiDarkBg
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(YankiDarkBg)
        ) {
            if (filteredBulletins.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Announcement,
                            contentDescription = null,
                            tint = YankiGreyDot,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Henüz duyuru yok", color = YankiGreyDot)
                    }
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
            Icon(Icons.Default.FilterList, contentDescription = "Filtrele", tint = Color.White)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(YankiCardBg)
        ) {
            val filters = listOf("ALL" to "Hepsi", "INFO" to "Bilgi", "NEED" to "İhtiyaç", "ALERT" to "Uyarı")
            filters.forEach { (key, label) ->
                DropdownMenuItem(
                    text = { Text(label, color = if (selectedFilter == key) YankiGreen else Color.White) },
                    onClick = { onFilterSelected(key); expanded = false }
                )
            }
        }
    }
}

@Composable
fun BulletinItem(bulletin: BulletinEntity) {
    val typeColor = when (bulletin.type) {
        "ALERT" -> Color(0xFFE74C3C)
        "NEED" -> Color(0xFF3498DB)
        else -> YankiGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = YankiCardBg),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                    fontSize = 16.sp,
                    color = Color.White
                )
                Surface(
                    color = typeColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = bulletin.type,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = typeColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = bulletin.content, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = YankiGreyDot, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatTimestamp(bulletin.timestamp),
                        fontSize = 12.sp,
                        color = YankiGreyDot
                    )
                }
                Text(
                    text = "${bulletin.hop_count} Hop",
                    fontSize = 12.sp,
                    color = YankiGreyDot,
                    fontWeight = FontWeight.Medium
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
        containerColor = YankiCardBg,
        title = { Text("Yeni Duyuru", color = Color.White) },
        text = {
            Column {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Mesajınız") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YankiGreen,
                        unfocusedBorderColor = YankiGreyDot,
                        focusedLabelColor = YankiGreen,
                        unfocusedLabelColor = YankiGreyDot,
                        cursorColor = YankiGreen,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Duyuru Tipi:", fontWeight = FontWeight.SemiBold, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val types = listOf("INFO" to "Bilgi", "NEED" to "İhtiyaç", "ALERT" to "Uyarı")
                    types.forEach { (key, label) ->
                        RadioButton(
                            selected = selectedType == key,
                            onClick = { selectedType = key },
                            colors = RadioButtonDefaults.colors(selectedColor = YankiGreen, unselectedColor = YankiGreyDot)
                        )
                        Text(label, color = Color.White, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (content.isNotBlank()) onPost(content, selectedType) },
                enabled = content.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = YankiGreen)
            ) {
                Text("Yayınla")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal", color = YankiGreyDot)
            }
        }
    )
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
