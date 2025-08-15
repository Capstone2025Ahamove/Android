package com.example.aidashboard

data class ChatSession(
    val id: String,
    val title: String,
    val threadId: String,
    val fileId: String,
    val messages: List<ChatMessage>,
    val createdAt: Long,
    val updatedAt: Long
)
