package com.example.aidashboard

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import kotlinx.coroutines.launch

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
                            navController.navigate("chat/${threadId}/${fileId}")
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    "chat/{threadId}/{fileId}",
                    arguments = listOf(
                        navArgument("threadId") { type = NavType.StringType },
                        navArgument("fileId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val threadId = backStackEntry.arguments?.getString("threadId") ?: ""
                    val fileId = backStackEntry.arguments?.getString("fileId") ?: ""

                    // Keep state consistent
                    chatThreadId = threadId
                    chatFileId = fileId

                    if (chatMessages.isEmpty() && !initialSummary.isNullOrBlank()) {
                        chatMessages = listOf(ChatMessage("assistant", initialSummary ?: ""))
                    }

                    ChatScreen(
                        threadId = threadId,
                        fileId = fileId,
                        initialSummary = initialSummary ?: "",
                        messages = chatMessages,
                        onMessagesChanged = { newMsgs -> chatMessages = newMsgs },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("history") {
                    var sessions by remember { mutableStateOf<List<ChatSession>>(emptyList()) }
                    var isLoading by remember { mutableStateOf(true) }
                    val scope = rememberCoroutineScope()

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
                            chatFileId = session.fileId
                            chatMessages = session.messages
                            initialSummary = session.messages.firstOrNull()?.content ?: ""
                            navController.navigate("chat/${session.threadId}/${session.fileId}")
                        },
                        onDeleteSession = { session ->
                            scope.launch {
                                ChatStore.delete(this@MainActivity, session.id)
                                sessions = ChatStore.getAll(this@MainActivity)
                            }
                        },

                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
