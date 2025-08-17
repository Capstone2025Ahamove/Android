package com.example.aidashboard

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.core.content.ContextCompat
import com.example.aidashboard.api.KPIAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KPIAnalysisScreen(
    onBack: () -> Unit,
    assistantId: String,
    apiKey: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedDepartment by remember { mutableStateOf("Marketing") }
    var predictionText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val departments = listOf(
        "Marketing", "Sales", "Tech", "Product", "Finance", "Operations", "Customer Support"
    )

    val filePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val tempFile = copyUriToTempFile(context, it)
                if (tempFile != null) {
                    isLoading = true
                    predictionText = ""
                    scope.launch(Dispatchers.IO) {
                        val analyzer = KPIAnalyzer(assistantId, apiKey)
                        val departmentKey = selectedDepartment.lowercase()
                        val historicalId = getHistoricalFileId(departmentKey)
                        val result = analyzer.analyzeKPI(selectedDepartment, tempFile, historicalId)
                        predictionText = result
                        isLoading = false
                    }
                }
            }
        }

    Scaffold(
        containerColor = Color(0xFFFFFCF7),
        // Ensures our layout respects display cutouts & system bars
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (predictionText.isNotBlank()) {
                Surface(color = Color.Transparent) {
                    // This padding keeps the button clear of the gesture/nav bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Button(
                            onClick = { saveToFile(context, predictionText) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00731D)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                "Download Report",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                // Apply innerPadding first so content never hides under the bottom bar
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF00731D))
            }

            Text(
                text = "KPI PREDICTION",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00731D),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 6.dp)
            )

            Text(
                text = "Forecast KPI performance\nbased on historical data",
                fontSize = 18.sp,
                color = Color(0xFF26914B),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DropdownMenuBox(
                    selected = selectedDepartment,
                    options = departments,
                    onSelected = { selectedDepartment = it }
                )

                Button(
                    onClick = { filePicker.launch("*/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00731D)),
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("KPI CSV file", color = Color.White)
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(vertical = 24.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }

            if (predictionText.isNotBlank()) {
                val innerScroll = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 250.dp, max = 400.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.5.dp, Color(0xFF00731D), RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(16.dp)
                        .verticalScroll(innerScroll)
                ) {
                    Text(
                        text = predictionText,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun DropdownMenuBox(
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00731D)),
            modifier = Modifier
                .height(48.dp)
                .border(1.5.dp, Color(0xFF00731D), RoundedCornerShape(50))
        ) {
            Text(selected)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        expanded = false
                        onSelected(item)
                    }
                )
            }
        }
    }
}

private fun copyUriToTempFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("kpi_", ".csv", context.cacheDir)
        FileOutputStream(tempFile).use { output -> inputStream.copyTo(output) }
        tempFile
    } catch (_: Exception) {
        null
    }
}

private fun saveToFile(context: Context, text: String) {
    val formatter = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
    val timestamp = formatter.format(Date())
    val fileName = "KPI_Prediction_$timestamp.txt"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            try {
                resolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
                Toast.makeText(context, "Report saved to Downloads", Toast.LENGTH_LONG).show()
            } catch (_: IOException) {
                Toast.makeText(context, "❌ Failed to write file", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "❌ Failed to create file", Toast.LENGTH_SHORT).show()
        }
    } else {
        val downloadsDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context, "Please grant storage permission", Toast.LENGTH_LONG).show()
            return
        }

        try {
            FileOutputStream(file).use { it.write(text.toByteArray()) }
            Toast.makeText(context, "Report saved to Downloads", Toast.LENGTH_LONG).show()
        } catch (_: IOException) {
            Toast.makeText(context, "❌ Failed to write file", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun getHistoricalFileId(department: String): String? {
    return mapOf(
        "marketing" to "file-TJYfXNfMKrPAttAKrUjmTk",
        "sales" to "file-N5tC9anVXgmgq41jDcnCjq",
        "tech" to "file-NPaZXz5EuToA2Pw24C1Ms5",
        "product" to "file-VRMzU8QMTasBxcyCPtDHZt",
        "finance" to "file-E7MQvXTgk1bRy4SBcttuwx",
        "operations" to "file-8KhZbBwixYyBcm5VpLBY8g",
        "customer support" to "file-UZttM4zsKqoVGiTNMPnW5J"
    )[department]
}
