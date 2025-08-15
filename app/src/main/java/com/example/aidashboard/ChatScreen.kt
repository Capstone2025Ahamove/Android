package com.example.aidashboard

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    threadId: String,
    fileId: String,
    initialSummary: String,
    messages: List<ChatMessage>,
    onMessagesChanged: (List<ChatMessage>) -> Unit,
    onBack: () -> Unit
) {
    var userInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Analyze Chat",
                        color = Color(0xFF00731D),
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF00731D))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFFCFAF5))
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                reverseLayout = false
            ) {
                items(messages) { msg ->
                    val isUser = msg.sender == "user"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        if (!isUser && msg == messages.first()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.94f)
                                    .border(2.dp, Color(0xFF00731D), RoundedCornerShape(16.dp))
                                    .background(Color.White, RoundedCornerShape(16.dp))
                                    .padding(18.dp)
                            ) {
                                Text(
                                    msg.content,
                                    color = Color.Black,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Normal,
                                    textAlign = TextAlign.Start
                                )
                            }
                        } else if (!isUser) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .border(1.5.dp, Color(0xFF00731D), RoundedCornerShape(16.dp))
                                    .background(Color.White, RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                            ) {
                                Text(
                                    msg.content,
                                    color = Color.Black,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Normal,
                                    textAlign = TextAlign.Start
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF00731D), RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                            ) {
                                Text(
                                    msg.content,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF00731D))
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFCFAF5))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    modifier = Modifier
                        .weight(1f)
                        .border(2.dp, Color(0xFF00731D), RoundedCornerShape(12.dp)),
                    placeholder = { Text("Continue conversation...") },
                    singleLine = true,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val messageText = userInput.trim()
                        if (messageText.isNotEmpty() && !isLoading) {
                            val updatedUserMessages = messages + ChatMessage("user", messageText)
                            onMessagesChanged(updatedUserMessages)
                            userInput = ""
                            isLoading = true

                            scope.launch(Dispatchers.IO) {
                                val sendSuccess = OpenAiRepository.sendChatMessage(threadId, messageText)
                                if (sendSuccess) {
                                    val runId = OpenAiRepository.runChatAssistant(threadId, fileId)
                                    if (runId != null) {
                                        val reply = OpenAiRepository.pollAndFetchChatMessage(threadId, runId)
                                        launch(Dispatchers.Main) {
                                            val updatedAllMessages = updatedUserMessages + ChatMessage("assistant", reply ?: "❌ No response from assistant.")
                                            onMessagesChanged(updatedAllMessages)

                                            val now = System.currentTimeMillis()
                                            val session = ChatSession(
                                                id = threadId,
                                                title = "Chat with Assistant",
                                                threadId = threadId,
                                                fileId = fileId,
                                                messages = updatedAllMessages,
                                                createdAt = now,
                                                updatedAt = now
                                            )
                                            ChatStore.upsert(context, session)
                                            isLoading = false
                                        }
                                    } else {
                                        launch(Dispatchers.Main) {
                                            val failMessages = updatedUserMessages + ChatMessage("assistant", "❌ Run failed.")
                                            onMessagesChanged(failMessages)

                                            val now = System.currentTimeMillis()
                                            val session = ChatSession(
                                                id = threadId,
                                                title = "Chat with Assistant",
                                                threadId = threadId,
                                                fileId = fileId,
                                                messages = failMessages,
                                                createdAt = now,
                                                updatedAt = now
                                            )
                                            ChatStore.upsert(context, session)
                                            isLoading = false
                                        }
                                    }
                                } else {
                                    launch(Dispatchers.Main) {
                                        val failMessages = updatedUserMessages + ChatMessage("assistant", "❌ Failed to send message.")
                                        onMessagesChanged(failMessages)

                                        val now = System.currentTimeMillis()
                                        val session = ChatSession(
                                            id = threadId,
                                            title = "Chat with Assistant",
                                            threadId = threadId,
                                            fileId = fileId,
                                            messages = failMessages,
                                            createdAt = now,
                                            updatedAt = now
                                        )
                                        ChatStore.upsert(context, session)
                                        isLoading = false
                                    }
                                }
                            }
                        }
                    },
                    enabled = userInput.isNotBlank() && !isLoading
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color(0xFF00731D))
                }
            }
        }
    }
}
