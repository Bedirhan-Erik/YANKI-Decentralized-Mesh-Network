package com.bedir.yanki.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bedir.yanki.ui.theme.*
import com.bedir.yanki.ui.viewmodel.MeshViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    userId: String,
    userName: String,
    viewModel: MeshViewModel,
    navController: NavController
) {
    var messageText by remember { mutableStateOf("") }
    val dbMessages by viewModel.getMessagesWithUser(userId).collectAsState(initial = emptyList())
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(dbMessages.size) {
        if (dbMessages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(dbMessages.size - 1)
            }
        }
    }

    val initials = userName.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(YankiGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = YankiGreen
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(YankiGreen, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "BLE Mesh Aktif",
                                    fontSize = 11.sp,
                                    color = YankiGreen
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = YankiCardBg,
                    titleContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = {
                        try { navController.navigateUp() } catch (e: Exception) { e.printStackTrace() }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        containerColor = YankiDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(YankiDarkBg, Color(0xFF0D1219))
                    )
                )
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(dbMessages) { msg ->
                    val contentString = try {
                        msg.content_blob?.let { String(it) } ?: ""
                    } catch (e: Exception) { "" }
                    val isMe = msg.sender_id == viewModel.repository.currentUserId
                    if (contentString.isNotBlank()) {
                        ChatBubble(
                            content = contentString,
                            isFromMe = isMe,
                            timestamp = msg.timestamp,
                            initials = if (!isMe) initials else ""
                        )
                    }
                }
            }

            ChatInputBar(
                text = messageText,
                onTextChange = { messageText = it },
                onSend = {
                    if (messageText.isNotBlank()) {
                        try {
                            viewModel.sendMessage(userId, messageText)
                            messageText = ""
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ChatBubble(content: String, isFromMe: Boolean, timestamp: Long, initials: String = "") {
    val time = remember(timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isFromMe) 48.dp else 0.dp,
                end = if (isFromMe) 0.dp else 48.dp
            ),
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isFromMe && initials.isNotBlank()) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(YankiGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = YankiGreen
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Column(horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start) {
            Surface(
                color = if (isFromMe) YankiGreen else YankiCardBg,
                shape = if (isFromMe)
                    RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
                else
                    RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                shadowElevation = 2.dp
            ) {
                Text(
                    text = content,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 21.sp
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = time,
                color = Color(0xFF5A6478),
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun ChatInputBar(text: String, onTextChange: (String) -> Unit, onSend: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = YankiCardBg,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = {
                    Text("Mesaj yazın...", color = Color(0xFF4A5568), fontSize = 15.sp)
                },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1E2A38)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E2A38),
                    unfocusedContainerColor = Color(0xFF1E2A38),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = YankiGreen
                ),
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (text.isNotBlank()) YankiGreen else Color(0xFF1E2A38)
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Gönder",
                    tint = if (text.isNotBlank()) Color.White else Color(0xFF4A5568)
                )
            }
        }
    }
}
