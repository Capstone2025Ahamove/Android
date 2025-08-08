package com.example.aidashboard

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()

            // All relevant state lifted here
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

                            // Clear previous chat and IDs on new analysis
                            chatMessages = listOf()
                            chatThreadId = null
                            chatFileId = null
                            initialSummary = null

                            navController.navigate("result")
                        }
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

                            // Start chat with summary if it's a fresh analysis
                            if (chatMessages.isEmpty()) {
                                chatMessages = listOf(ChatMessage("assistant", summary))
                            }
                            navController.navigate("chat")
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("chat") {
                    // Ensure summary appears only for new chat
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
            }
        }
    }
}
