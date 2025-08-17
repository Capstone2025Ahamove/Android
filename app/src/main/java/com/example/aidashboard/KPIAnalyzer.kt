package com.example.aidashboard.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.File

class KPIAnalyzer(private val assistantId: String, private val apiKey: String) {

    private val client = OkHttpClient()

    suspend fun analyzeKPI(
        departmentName: String,
        currentFile: File,
        historicalFileId: String? = null
    ): String = withContext(Dispatchers.IO) {
        try {
            val fileId = uploadFile(currentFile) ?: return@withContext "❌ File upload failed."
            val threadId = createThread() ?: return@withContext "❌ Thread creation failed."

            val prompt = """
            You are an expert KPI prediction assistant. Two files are attached:
            - One is the current month’s KPI for the $departmentName department.
            - The other contains last year’s trends for the same department.

            Your job:
            1. Calculate each KPI’s current value from raw data.
            2. Compare it with the same KPI from the same month last year.
            3. Use BSC targets to determine: Prediction = Meet | Not Meet
            4. Explain the prediction based on trends and targets.

            Output format:

            KPI: <name>
            Current Value: <value>
            Last Year (Same Month): <value>
            Target: <target>
            Prediction: Meet | Not Meet
            Reason: <reason>
        """.trimIndent()

            sendMessage(threadId, fileId, prompt, historicalFileId)
            val runId = startRun(threadId, fileId, historicalFileId) ?: return@withContext "❌ Run failed."
            val runSuccess = pollRunStatus(threadId, runId)
            if (!runSuccess) return@withContext "❌ GPT Run failed or timed out."

            return@withContext fetchResponse(threadId) ?: "❌ Failed to fetch GPT response."
        } catch (e: Exception) {
            e.printStackTrace() // Show full stack trace in Logcat
            Log.e("KPIAnalyzer", "❌ Exception during KPI analysis: ${e.message}")
            return@withContext "❌ Run failed.\n${e.message}"
        }
    }

    // Upload user KPI CSV
    private fun uploadFile(file: File): String? {
        Log.d("KPIAnalyzer", "Uploading file: ${file.name}")
        val mediaType = "text/csv".toMediaTypeOrNull()

        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("purpose", "assistants")
            .addFormDataPart("file", file.name, RequestBody.create(mediaType, file))
            .build()

        val request = Request.Builder()
            .url("https://api.openai.com/v1/files")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        return try {
            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: return null)
            json.getString("id")
        } catch (e: Exception) {
            null
        }
    }

    // Create a new thread
    private fun createThread(): String? {
        val request = Request.Builder()
            .url("https://api.openai.com/v1/threads")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("OpenAI-Beta", "assistants=v2")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), "{}"))
            .build()

        return try {
            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: return null)
            json.getString("id")
        } catch (e: Exception) {
            null
        }
    }

    // Send prompt with both uploaded file and historical file (if available)
    private fun sendMessage(threadId: String, fileId: String, prompt: String, historicalFileId: String?) {
        val attachments = mutableListOf(
            mapOf("file_id" to fileId, "tools" to listOf(mapOf("type" to "code_interpreter")))
        )
        historicalFileId?.let {
            attachments.add(mapOf("file_id" to it, "tools" to listOf(mapOf("type" to "code_interpreter"))))
        }

        val message = mapOf(
            "role" to "user",
            "content" to listOf(mapOf("type" to "text", "text" to prompt)),
            "attachments" to attachments
        )

        val json = JSONObject(message).toString()

        val request = Request.Builder()
            .url("https://api.openai.com/v1/threads/$threadId/messages")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("OpenAI-Beta", "assistants=v2")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), json))
            .build()

        client.newCall(request).execute().close()
    }

    // Start assistant run with required file resources
    private fun startRun(threadId: String, fileId: String, historicalFileId: String?): String? {
        val fileIds = mutableListOf(fileId)
        historicalFileId?.let { fileIds.add(it) }

        val payload = mapOf(
            "assistant_id" to assistantId,
            "tool_resources" to mapOf(
                "code_interpreter" to mapOf("file_ids" to fileIds)
            )
        )

        val json = JSONObject(payload).toString()
        Log.d("KPIAnalyzer", "📤 Run Request JSON: $json")

        val request = Request.Builder()
            .url("https://api.openai.com/v1/threads/$threadId/runs")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("OpenAI-Beta", "assistants=v2")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), json))
            .build()

        return try {
            val response = client.newCall(request).execute()
            val bodyString = response.body?.string()
            Log.d("KPIAnalyzer", "✅ Run Response: $bodyString")

            if (!response.isSuccessful) {
                Log.e("KPIAnalyzer", "❌ Run request failed with HTTP ${response.code}")
                return null
            }

            val jsonResp = JSONObject(bodyString ?: return null)
            jsonResp.getString("id")
        } catch (e: Exception) {
            Log.e("KPIAnalyzer", "❌ Exception in startRun: ${e.message}")
            null
        }
    }


    // Poll run status until complete
    private fun pollRunStatus(threadId: String, runId: String): Boolean {
        val request = Request.Builder()
            .url("https://api.openai.com/v1/threads/$threadId/runs/$runId")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("OpenAI-Beta", "assistants=v2")
            .get()
            .build()

        repeat(20) {
            Thread.sleep(2000)
            try {
                val response = client.newCall(request).execute()
                val json = JSONObject(response.body?.string() ?: return false)
                when (json.getString("status")) {
                    "completed" -> return true
                    "failed" -> return false
                }
            } catch (e: Exception) {
                return false
            }
        }
        return false
    }

    // Retrieve GPT assistant's final message
    private fun fetchResponse(threadId: String): String? {
        val request = Request.Builder()
            .url("https://api.openai.com/v1/threads/$threadId/messages")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("OpenAI-Beta", "assistants=v2")
            .get()
            .build()

        return try {
            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: return null)
            val messages = json.getJSONArray("data")
            if (messages.length() == 0) return null
            val first = messages.getJSONObject(0)
            val contentArray = first.getJSONArray("content")
            val textObj = contentArray.getJSONObject(0).getJSONObject("text")
            textObj.getString("value")
        } catch (e: Exception) {
            null
        }

    }

}
