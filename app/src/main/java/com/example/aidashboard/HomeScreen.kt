package com.example.aidashboard

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable

fun HomeScreen(
    onStartAnalyze: (Uri, Boolean) -> Unit,
    onOpenHistory: () -> Unit = {}       // <-- add this
)
 {
    var pickedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pickedSheetUri by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pickedPhotoUri = uri.also { if (it != null) pickedSheetUri = null }
    }
    val sheetPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pickedSheetUri = uri.also { if (it != null) pickedPhotoUri = null }
    }

    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFCFAF5))
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(50))
                    .border(1.5.dp, Color(0xFF00731D), RoundedCornerShape(50))
                    .clickable { onOpenHistory() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    tint = Color(0xFF00731D),
                    modifier = Modifier.size(28.dp)
                )
            }
            Row {
//                IconButton(onClick = { }) {
//                    Icon(Icons.Default.Download, null, tint = Color(0xFF00731D))
//                }
                IconButton(onClick = { }) {
                    Icon(Icons.Default.MoreVert, null, tint = Color(0xFF00731D))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Title
        Text("AI DASHBOARD", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00731D))
        Text("INTERPRETER", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00731D))
        Text(
            "Generate summaries and insights\nfrom images or spreadsheets",
            fontSize = 16.sp,
            color = Color(0xFF26914B),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(Modifier.height(32.dp))

        // Upload Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(2.dp, Color(0xFF00731D), RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .clickable { photoPicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Photo, contentDescription = null, tint = Color(0xFF00731D), modifier = Modifier.size(44.dp))
                }
                Text("Upload photo", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF00731D), modifier = Modifier.padding(top = 6.dp))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(2.dp, Color(0xFF00731D), RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .clickable { sheetPicker.launch("*/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, tint = Color(0xFF00731D), modifier = Modifier.size(44.dp))
                }
                Text("Upload spreadsheet", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF00731D), modifier = Modifier.padding(top = 6.dp))
            }
        }

        Spacer(Modifier.height(32.dp))

        // File Preview
        if (pickedPhotoUri != null || pickedSheetUri != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFF5FFF5))
                    .border(2.dp, Color(0xFF00731D), RoundedCornerShape(24.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (pickedPhotoUri != null) {
                    AsyncImage(
                        model = pickedPhotoUri,
                        contentDescription = null,
                        modifier = Modifier
                            .width(220.dp)
                            .height(130.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )

                } else {
                    Text(
                        text = pickedSheetUri?.lastPathSegment ?: "Unknown file",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF00731D),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (pickedPhotoUri != null) photoPicker.launch("image/*")
                            else sheetPicker.launch("*/*")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00731D),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .defaultMinSize(minWidth = 100.dp)
                    ) {
                        Text("Update", fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.width(16.dp))

                    Button(
                        onClick = {
                            pickedPhotoUri = null
                            pickedSheetUri = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE74C3C),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .defaultMinSize(minWidth = 100.dp)
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        Spacer(Modifier.height(12.dp)) // Reduce push to bottom

        // Start Analyze Button
        Button(
            onClick = { isLoading = true },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00731D),
                contentColor = Color.White
            )
        ) {
            Text(
                text = if (isLoading) "Analyzing..." else "Start Analyze",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (isLoading) {
            LaunchedEffect(isLoading) {
                OpenAiRepository.analyzeFile(
                    context, pickedPhotoUri ?: pickedSheetUri!!,
                    isImage = pickedPhotoUri != null
                ) { result ->
                    isLoading = false
                    onStartAnalyze(pickedPhotoUri ?: pickedSheetUri!!, pickedPhotoUri != null)
                }
            }

            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator(
                color = Color(0xFF00731D),
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(16.dp))
        } else {
            Spacer(Modifier.height(20.dp))
        }
    }
}
