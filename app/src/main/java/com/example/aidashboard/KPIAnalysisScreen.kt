package com.example.aidashboard

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.aidashboard.api.KPIAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

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

    val departments = listOf("Marketing", "Sales", "Tech", "Product", "Finance", "Operations", "Customer Support")

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFCF7))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF00731D))
            }
            Text(
                text = "KPI Prediction",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF00731D),
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Text(
            text = "Forecast KPI performance\nbased on historical data",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF00731D),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
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
                modifier = Modifier.height(48.dp)
            ) {
                Text("KPI CSV file", color = Color.White)
            }
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        }

        if (predictionText.isNotBlank()) {
            Text(
                text = predictionText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color.White)
                    .padding(16.dp),
                color = Color.Black
            )

            Button(
                onClick = { saveToFile(context, predictionText) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00731D)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            ) {
                Text("Download Report", color = Color.White)
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
    val interactionSource = remember { MutableInteractionSource() }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.height(48.dp)
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
    } catch (e: Exception) {
        null
    }
}

private fun saveToFile(context: Context, text: String) {
    val time = System.currentTimeMillis()
    val file = File(
        context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
        "KPI_Prediction_$time.txt"
    )
    file.writeText(text)
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
