package com.example.aidashboard

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()

            var uploadedFileUri by remember { mutableStateOf<Uri?>(null) }
            var isImage by remember { mutableStateOf(false) }

            var chatThreadId by remember { mutableStateOf<String?>(null) }
            var chatFileId by remember { mutableStateOf<String?>(null) }
            var initialSummary by remember { mutableStateOf<String?>(null) }
            var chatMessages by remember { mutableStateOf(listOf<ChatMessage>()) }

            NavHost(navController = navController, startDestination = "home") {
                composable("home") {
                    HomeScreen(
                        onStartAnalyze = { uri, imageFlag ->
                            uploadedFileUri = uri
                            isImage = imageFlag
                            chatMessages = listOf()
                            chatThreadId = null
                            chatFileId = null
                            initialSummary = null
                            navController.navigate("result")
                        },
                        onOpenHistory = { navController.navigate("history") }
                    )
                }
                composable("result") {
                    ResultScreen(
                        context = this@MainActivity,
                        uri = uploadedFileUri!!,
                        isImage = isImage,
                        onChat = { threadId, fileId, summary ->
                            chatThreadId = threadId
                            chatFileId = fileId
                            initialSummary = summary
                            if (chatMessages.isEmpty()) {
                                chatMessages = listOf(ChatMessage("assistant", summary))
                            }
                            navController.navigate("chat")
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("chat") {
                    if (chatMessages.isEmpty() && !initialSummary.isNullOrBlank()) {
                        chatMessages = listOf(ChatMessage("assistant", initialSummary ?: ""))
                    }
                    ChatScreen(
                        threadId = chatThreadId ?: "",
                        fileId = chatFileId ?: "",
                        initialSummary = initialSummary ?: "",
                        messages = chatMessages,
                        onMessagesChanged = { newMsgs -> chatMessages = newMsgs },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("history") {
                    var sessions by remember { mutableStateOf<List<ChatSession>>(emptyList()) }
                    var isLoading by remember { mutableStateOf(true) }

                    // Load sessions when this screen appears
                    LaunchedEffect(Unit) {
                        isLoading = true
                        sessions = ChatStore.getAll(this@MainActivity)
                        isLoading = false
                    }

                    HistoryScreen(
                        isLoading = isLoading,
                        sessions = sessions,
                        onOpenSession = { session ->
                            chatThreadId = session.threadId
                            chatMessages = session.messages
                            initialSummary = session.messages.firstOrNull()?.content ?: ""
                            chatFileId = null // Optional
                            navController.navigate("chat")
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
