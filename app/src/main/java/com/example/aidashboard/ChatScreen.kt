package com.example.aidashboard

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.aidashboard.ChatMessage



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
                                    .border(
                                        width = 2.dp,
                                        color = Color(0xFF00731D),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .background(Color.White, shape = RoundedCornerShape(16.dp))
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
                                    .border(
                                        width = 1.5.dp,
                                        color = Color(0xFF00731D),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .background(Color.White, shape = RoundedCornerShape(16.dp))
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
                                    .background(Color(0xFF00731D), shape = RoundedCornerShape(16.dp))
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
                        .border(
                            width = 2.dp,
                            color = Color(0xFF00731D),
                            shape = RoundedCornerShape(12.dp)
                        ),
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
                            onMessagesChanged(messages + ChatMessage("user", messageText))
                            userInput = ""
                            isLoading = true

                            scope.launch(Dispatchers.IO) {
                                val sendSuccess = OpenAiRepository.sendChatMessage(threadId, messageText)
                                if (sendSuccess) {
                                    val runId = OpenAiRepository.runChatAssistant(threadId, fileId)
                                    if (runId != null) {
                                        val reply = OpenAiRepository.pollAndFetchChatMessage(threadId, runId)
                                        launch(Dispatchers.Main) {
                                            if (!reply.isNullOrBlank()) {
                                                onMessagesChanged(messages + ChatMessage("user", messageText) + ChatMessage("assistant", reply))
                                            } else {
                                                onMessagesChanged(messages + ChatMessage("user", messageText) + ChatMessage("assistant", "❌ No response from assistant."))
                                            }
                                            isLoading = false
                                        }
                                    } else {
                                        launch(Dispatchers.Main) {
                                            onMessagesChanged(messages + ChatMessage("user", messageText) + ChatMessage("assistant", "❌ Run failed."))
                                            isLoading = false
                                        }
                                    }
                                } else {
                                    launch(Dispatchers.Main) {
                                        onMessagesChanged(messages + ChatMessage("user", messageText) + ChatMessage("assistant", "❌ Failed to send message."))
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
