package com.example.aidashboard

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.ContextCompat

@Composable
fun ResultScreen(
    context: Context,
    uri: android.net.Uri,
    isImage: Boolean,
    onChat: (threadId: String, fileId: String, summary: String) -> Unit,
    onBack: () -> Unit
) {
    val summaryAssistantId = "asst_lYMPOqnXe86rZ2oPqe6N3bx2"
    val insightAssistantId = "asst_LIuWUGUi5ClNpJMuEAbYQsRs"

    // IMPORTANT: persist across recompositions & back stack restores
    var summary by rememberSaveable { mutableStateOf("Loading summary...") }
    var insights by rememberSaveable { mutableStateOf("Loading key insights...") }
    var isLoading by rememberSaveable { mutableStateOf(true) }
    var threadId by rememberSaveable { mutableStateOf<String?>(null) }
    var fileId by rememberSaveable { mutableStateOf<String?>(null) }
    var didAnalyze by rememberSaveable { mutableStateOf(false) }

    // Run analysis exactly once per screen lifetime
    LaunchedEffect(Unit) {
        if (didAnalyze) return@LaunchedEffect
        didAnalyze = true

        isLoading = true
        summary = "Loading summary..."
        insights = "Loading key insights..."

        // First call returns threadId + fileId that we will REUSE for chat
        OpenAiRepository.analyzeWithAssistantAndReturnIds(
            context, uri, isImage, summaryAssistantId
        ) { result, tId, fId ->
            summary = result
            threadId = tId
            fileId = fId
        }

        // Insights may run in parallel; it doesn't change thread/file IDs
        OpenAiRepository.analyzeWithAssistant(
            context, uri, isImage, insightAssistantId
        ) { result ->
            insights = result
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFCFAF5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar (Back + Download)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Back", tint = Color(0xFF00731D))
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {
                    val reportText = "Summary:\n$summary\n\nKey Insights:\n$insights"
                    downloadTextFile(context, reportText)
                }) {
                    Icon(Icons.Default.Download, contentDescription = "Download", tint = Color(0xFF00731D))
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Summary",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00731D),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                textAlign = TextAlign.Center
            )

            if (isLoading) {
                Spacer(Modifier.height(32.dp))
                CircularProgressIndicator(color = Color(0xFF00731D), modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Loading...", color = Color(0xFF00731D), fontWeight = FontWeight.SemiBold)
            } else {
                // Summary Section Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .border(2.dp, Color(0xFF00731D), RoundedCornerShape(16.dp))
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(18.dp)
                ) {
                    Text(
                        text = summary,
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Key Insights",
                    fontSize = 28.sp,
                    color = Color(0xFF00731D),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    textAlign = TextAlign.Center
                )
                // Insights Section Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, Color(0xFF00731D), RoundedCornerShape(16.dp))
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(18.dp)
                ) {
                    Text(
                        text = insights,
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.weight(1f))
            // Download & Chat row at the bottom
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        val reportText = "Summary:\n$summary\n\nKey Insights:\n$insights"
                        downloadTextFile(context, reportText)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00731D), contentColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Download Report", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(16.dp))
                IconButton(
                    onClick = {
                        if (threadId != null && fileId != null) {
                            onChat(
                                threadId!!,
                                fileId!!,
                                summary + "\n\nIf you have any more questions, feel free to ask me 🙂"
                            )
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White, RoundedCornerShape(24.dp))
                        .border(2.dp, Color(0xFF00731D), RoundedCornerShape(24.dp)),
                    enabled = (threadId != null && fileId != null)
                ) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Chat", tint = Color(0xFF00731D))
                }
            }
        }
    }
}

fun downloadTextFile(context: Context, content: String) {
    val fileName = "AI_Report.txt"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(android.provider.MediaStore.Downloads.RELATIVE_PATH, "Download/")
        }
        val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            resolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content.toByteArray())
                Toast.makeText(context, "Report saved to Downloads", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "❌ Failed to save report", Toast.LENGTH_SHORT).show()
        }
    } else {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context, "Please grant storage permission in settings", Toast.LENGTH_LONG).show()
            return
        }
        try {
            FileOutputStream(file).use { it.write(content.toByteArray()) }
            Toast.makeText(context, "Report saved to Downloads", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "❌ Failed to save report", Toast.LENGTH_SHORT).show()
        }
    }
}
