package com.bedir.yanki.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bedir.yanki.ui.theme.*
import com.bedir.yanki.ui.viewmodel.MeshViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    userId: String,
    userName: String,
    viewModel: MeshViewModel,
    navController: NavController
) {
    var messageText by remember { mutableStateOf("") }

    // 1. BOT MESAJLARI SİLİNDİ! Sadece veritabanından gelen canlı veriyi dinliyoruz.
    // Eğer veritabanında mesaj yoksa ekran temiz ve boş görünür.
    val dbMessages by viewModel.getMessagesWithUser(userId).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = userName, style = MaterialTheme.typography.titleMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                navigationIcon = {
                    IconButton(onClick = {
                        // 2. ÇÖKME KORUMASI: Geri çıkış zırhlandı
                        try {
                            navController.navigateUp()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF5F5F5))
        ) {

            // --- SOHBET LİSTESİ ---
            LazyColumn(
                modifier = Modifier
                    .weight(1f) // Textbox'ı en alta itmesi için
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Veritabanındaki mesajları ekrana basıyoruz
                items(dbMessages) { msg ->
                    // 3. ÇÖKME KORUMASI: ByteArray dönüşümünde Null kontrolü!
                    val contentString = try {
                        msg.content_blob?.let { String(it) } ?: ""
                    } catch (e: Exception) {
                        "Hatalı Mesaj Formatı"
                    }

                    val isMe = msg.sender_id == viewModel.repository.currentUserId

                    // Sadece içi dolu mesajları ekrana çiz
                    if (contentString.isNotBlank()) {
                        ChatBubble(content = contentString, isFromMe = isMe)
                    }
                }
            }

            // --- MESAJ YAZMA ALANI ---
            ChatInputBar(
                text = messageText,
                onTextChange = { messageText = it },
                onSend = {
                    if (messageText.isNotBlank()) {
                        // 4. ÇÖKME KORUMASI: Gönderim işlemi çift katmanlı zırhlandı
                        try {
                            viewModel.sendMessage(userId, messageText)
                            messageText = "" // Gönderdikten sonra kutuyu temizle
                        } catch (e: Exception) {
                            // UI tarafında bir hata oluşursa yakala ve logla
                            e.printStackTrace()
                        }
                    }
                }
            )
        }
    }
}

// DİKKAT: ChatBubble parametreleri değişti! Artık ChatMessage nesnesi değil, direkt String ve Boolean alıyor.
@Composable
fun ChatBubble(content: String, isFromMe: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = if (isFromMe) YankiDarkBg else Color.White,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 2.dp
        ) {
            Text(
                text = content,
                modifier = Modifier.padding(12.dp),
                color = if (isFromMe) Color.White else Color.Black,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun ChatInputBar(text: String, onTextChange: (String) -> Unit, onSend: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Mesaj yazın...") },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            IconButton(onClick = onSend) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gönder", tint = YankiGreen)
            }
        }
    }
}