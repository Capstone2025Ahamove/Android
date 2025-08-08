package com.example.aidashboard

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
import okio.BufferedSink
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.InputStream
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody


object OpenAiRepository {
    private const val OPENAI_API_KEY = com.example.aidashboard.BuildConfig.OPENAI_API_KEY
    private const val ASSISTANT_ID = "asst_p0ajNzziydcju4E9O37JLWtt"
    private const val BASE_URL = "https://api.openai.com/v1"
    private val client = OkHttpClient()

    private fun postToMain(block: () -> Unit) {
        Handler(Looper.getMainLooper()).post { block() }
    }

    // --- Upload file ---
    fun uploadFile(context: Context, uri: Uri, onResult: (String?) -> Unit) {
        // FIX: Ensure fileName always has a valid image extension
        val fileName = (uri.lastPathSegment ?: "uploadfile.jpg").let {
            if (it.endsWith(".jpg", true) || it.endsWith(".jpeg", true) || it.endsWith(".png", true) || it.endsWith(".gif", true) || it.endsWith(".webp", true)) {
                it
            } else {
                "$it.jpg"
            }
        }
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        if (inputStream == null) {
            Log.e("OpenAiRepository", "uploadFile: Failed to open input stream for $uri")
            postToMain { onResult(null) }
            return
        }

        val requestBody = object : RequestBody() {
            override fun contentType() = "application/octet-stream".toMediaTypeOrNull()
            override fun writeTo(sink: BufferedSink) {
                inputStream.use { it.copyTo(sink.outputStream()) }
            }
        }

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("purpose", "assistants")
            .addFormDataPart("file", fileName, requestBody)
            .build()

        val request = Request.Builder()
            .url("$BASE_URL/files")
            .addHeader("Authorization", "Bearer $OPENAI_API_KEY")
            .post(multipartBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                Log.e("OpenAiRepository", "uploadFile onFailure: ${e.localizedMessage}")
                postToMain { onResult(null) }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = response.body?.string()
                    val fid = JSONObject(body ?: "{}").optString("id", null)
                    if (!response.isSuccessful) {
                        Log.e("OpenAiRepository", "uploadFile onResponse error: $body")
                    }
                    postToMain { onResult(fid) }
                }
            }
        })
    }

    // --- Create thread ---
    fun createThread(onResult: (String?) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/threads")
            .addHeader("Authorization", "Bearer $OPENAI_API_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("OpenAI-Beta", "assistants=v2")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), "{}"))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                Log.e("OpenAiRepository", "createThread onFailure: ${e.localizedMessage}")
                postToMain { onResult(null) }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = response.body?.string()
                    val tid = JSONObject(body ?: "{}").optString("id", null)
                    if (!response.isSuccessful) {
                        Log.e("OpenAiRepository", "createThread onResponse error: $body")
                    }
                    postToMain { onResult(tid) }
                }
            }
        })
    }

    // --- Add message (text or image) ---
    fun addMessage(threadId: String, fileId: String, isImage: Boolean, onResult: (Boolean) -> Unit) {
        val payload = if (isImage) {
            """
            {
                "role": "user",
                "content": [
                    {"type": "image_file", "image_file": {"file_id": "$fileId"}}
                ]
            }
            """
        } else {
            """
            {
                "role": "user",
                "content": [
                    {"type": "text", "text": "This spreadsheet contains dashboard KPIs. Please analyze the key trends and provide a short 3–4 bullet point summary."}
                ]
            }
            """
        }

        val request = Request.Builder()
            .url("$BASE_URL/threads/$threadId/messages")
            .addHeader("Authorization", "Bearer $OPENAI_API_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("OpenAI-Beta", "assistants=v2")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), payload))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                Log.e("OpenAiRepository", "addMessage onFailure: ${e.localizedMessage}")
                postToMain { onResult(false) }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        postToMain { onResult(true) }
                    } else {
                        val errorBody = response.body?.string()
                        Log.e("OpenAiRepository", "AddMessage error: $errorBody")
                        postToMain { onResult(false) }
                    }
                }
            }
        })
    }

    // --- Run assistant ---
    fun runAssistant(threadId: String, fileId: String?, onResult: (String?) -> Unit) {
        val payload = if (fileId != null) {
            """
            {
                "assistant_id": "$ASSISTANT_ID",
                "tool_resources": {
                    "code_interpreter": {"file_ids": ["$fileId"]}
                }
            }
            """
        } else {
            """
            {
                "assistant_id": "$ASSISTANT_ID"
            }
            """
        }

        val request = Request.Builder()
            .url("$BASE_URL/threads/$threadId/runs")
            .addHeader("Authorization", "Bearer $OPENAI_API_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("OpenAI-Beta", "assistants=v2")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), payload))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                Log.e("OpenAiRepository", "runAssistant onFailure: ${e.localizedMessage}")
                postToMain { onResult(null) }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = response.body?.string()
                    val runId = JSONObject(body ?: "{}").optString("id", null)
                    if (!response.isSuccessful) {
                        Log.e("OpenAiRepository", "runAssistant onResponse error: $body")
                    }
                    postToMain { onResult(runId) }
                }
            }
        })
    }

    // --- Poll run status and fetch result ---
    fun pollRunAndFetch(threadId: String, runId: String, onResult: (String?) -> Unit) {
        fun poll() {
            val request = Request.Builder()
                .url("$BASE_URL/threads/$threadId/runs/$runId")
                .addHeader("Authorization", "Bearer $OPENAI_API_KEY")
                .addHeader("OpenAI-Beta", "assistants=v2")
                .get()
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: java.io.IOException) {
                    Log.e("OpenAiRepository", "pollRunAndFetch onFailure: ${e.localizedMessage}")
                    postToMain { onResult(null) }
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val body = response.body?.string()
                        val status = JSONObject(body ?: "{}").optString("status", "unknown")
                        if (!response.isSuccessful) {
                            Log.e("OpenAiRepository", "pollRunAndFetch onResponse error: $body")
                        }
                        if (status == "completed") {
                            fetchMessages(threadId, onResult)
                        } else if (status == "failed") {
                            postToMain { onResult("❌ Assistant run failed.") }
                        } else {
                            CoroutineScope(Dispatchers.IO).launch {
                                delay(2000)
                                poll()
                            }
                        }
                    }
                }
            })
        }
        poll()
    }

    // --- Fetch result message ---
    fun fetchMessages(threadId: String, onResult: (String?) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/threads/$threadId/messages")
            .addHeader("Authorization", "Bearer $OPENAI_API_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("OpenAI-Beta", "assistants=v2")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                Log.e("OpenAiRepository", "fetchMessages onFailure: ${e.localizedMessage}")
                postToMain { onResult("❌ Failed to fetch result.") }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = response.body?.string()
                    if (!response.isSuccessful) {
                        Log.e("OpenAiRepository", "fetchMessages onResponse error: $body")
                    }
                    val msgs = JSONObject(body ?: "{}").optJSONArray("data")
                    val firstMsg = msgs?.optJSONObject(0)
                    val contentArr = firstMsg?.optJSONArray("content")
                    val textObj = contentArr?.optJSONObject(0)?.optJSONObject("text")
                    val value = textObj?.optString("value")
                    postToMain { onResult(value ?: "❌ Failed to parse result.") }
                }
            }
        })
    }

    // --- HIGH LEVEL: Complete pipeline call ---
    fun analyzeFile(
        context: Context,
        uri: Uri,
        isImage: Boolean,
        onResult: (String) -> Unit
    ) {
        uploadFile(context, uri) { fileId ->
            if (fileId == null) { onResult("❌ File upload failed."); return@uploadFile }
            createThread { threadId ->
                if (threadId == null) { onResult("❌ Thread creation failed."); return@createThread }
                addMessage(threadId, fileId, isImage) { ok ->
                    if (!ok) { onResult("❌ Message send failed."); return@addMessage }
                    runAssistant(threadId, if (!isImage) fileId else null) { runId ->
                        if (runId == null) { onResult("❌ Assistant run failed."); return@runAssistant }
                        pollRunAndFetch(threadId, runId) { result ->
                            onResult(result ?: "❌ Unknown error.")
                        }
                    }
                }
            }
        }
    }



    // --- HIGH LEVEL: Complete pipeline call with assistant id ---
    fun analyzeWithAssistant(
        context: Context,
        uri: Uri,
        isImage: Boolean,
        assistantId: String,
        onResult: (String) -> Unit
    ) {
        uploadFile(context, uri) { fileId ->
            if (fileId == null) { onResult("❌ File upload failed."); return@uploadFile }
            createThread { threadId ->
                if (threadId == null) { onResult("❌ Thread creation failed."); return@createThread }
                addMessage(threadId, fileId, isImage) { ok ->
                    if (!ok) { onResult("❌ Message send failed."); return@addMessage }
                    // This is the key: use the provided assistantId!
                    runAssistantWithId(threadId, assistantId, if (!isImage) fileId else null) { runId ->
                        if (runId == null) { onResult("❌ Assistant run failed."); return@runAssistantWithId }
                        pollRunAndFetch(threadId, runId) { result ->
                            onResult(result ?: "❌ Unknown error.")
                        }
                    }
                }
            }
        }
    }

    // Copy of runAssistant but with custom assistantId
    fun runAssistantWithId(threadId: String, assistantId: String, fileId: String?, onResult: (String?) -> Unit) {
        val payload = if (fileId != null) {
            """
        {
            "assistant_id": "$assistantId",
            "tool_resources": {
                "code_interpreter": {"file_ids": ["$fileId"]}
            }
        }
        """
        } else {
            """
        {
            "assistant_id": "$assistantId"
        }
        """
        }

        val request = Request.Builder()
            .url("$BASE_URL/threads/$threadId/runs")
            .addHeader("Authorization", "Bearer $OPENAI_API_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("OpenAI-Beta", "assistants=v2")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), payload))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                Log.e("OpenAiRepository", "runAssistantWithId onFailure: ${e.localizedMessage}")
                postToMain { onResult(null) }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = response.body?.string()
                    val runId = JSONObject(body ?: "{}").optString("id", null)
                    if (!response.isSuccessful) {
                        Log.e("OpenAiRepository", "runAssistantWithId onResponse error: $body")
                    }
                    postToMain { onResult(runId) }
                }
            }
        })
    }
    // High-level: Complete pipeline call with assistantId, also returns threadId and fileId
    fun analyzeWithAssistantAndReturnIds(
        context: Context,
        uri: Uri,
        isImage: Boolean,
        assistantId: String,
        onResult: (String, String, String) -> Unit // result, threadId, fileId
    ) {
        uploadFile(context, uri) { fileId ->
            if (fileId == null) { onResult("❌ File upload failed.", "", ""); return@uploadFile }
            createThread { threadId ->
                if (threadId == null) { onResult("❌ Thread creation failed.", "", ""); return@createThread }
                addMessage(threadId, fileId, isImage) { ok ->
                    if (!ok) { onResult("❌ Message send failed.", "", ""); return@addMessage }
                    runAssistantWithId(threadId, assistantId, if (!isImage) fileId else null) { runId ->
                        if (runId == null) { onResult("❌ Assistant run failed.", "", ""); return@runAssistantWithId }
                        pollRunAndFetch(threadId, runId) { result ->
                            onResult(result ?: "❌ Unknown error.", threadId, fileId)
                        }
                    }
                }
            }
        }
    }

    // Sends a user message to an existing thread (returns true if successful)
    suspend fun sendChatMessage(threadId: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = """
            {
                "role": "user",
                "content": [
                    {"type": "text", "text": ${JSONObject.quote(content)}}
                ]
            }
        """.trimIndent()
            val request = Request.Builder()
                .url("https://api.openai.com/v1/threads/$threadId/messages")
                .addHeader("Authorization", "Bearer $OPENAI_API_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("OpenAI-Beta", "assistants=v2")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    // Kicks off an assistant run (returns runId)
    suspend fun runChatAssistant(threadId: String, fileId: String?): String? = withContext(Dispatchers.IO) {
        try {
            val payload = if (fileId != null) {
                """
                {
                    "assistant_id": "$ASSISTANT_ID",
                    "tool_resources": {
                        "code_interpreter": {"file_ids": ["$fileId"]}
                    }
                }
            """.trimIndent()
            } else {
                """
                {
                    "assistant_id": "$ASSISTANT_ID"
                }
            """.trimIndent()
            }
            val request = Request.Builder()
                .url("https://api.openai.com/v1/threads/$threadId/runs")
                .addHeader("Authorization", "Bearer $OPENAI_API_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("OpenAI-Beta", "assistants=v2")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            if (response.isSuccessful && body != null) {
                JSONObject(body).optString("id", null)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    // Poll for assistant reply, return the latest message string
    suspend fun pollAndFetchChatMessage(threadId: String, runId: String): String? = withContext(Dispatchers.IO) {
        // (You can refactor your pollRunAndFetch and fetchMessages for reuse here)
        // Here is a simple polling version:
        repeat(15) {  // up to ~30s
            delay(2000)
            val request = Request.Builder()
                .url("https://api.openai.com/v1/threads/$threadId/runs/$runId")
                .addHeader("Authorization", "Bearer $OPENAI_API_KEY")
                .addHeader("OpenAI-Beta", "assistants=v2")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val status = JSONObject(response.body?.string() ?: "{}").optString("status", "unknown")
            if (status == "completed") {
                // Get messages
                val msgReq = Request.Builder()
                    .url("https://api.openai.com/v1/threads/$threadId/messages")
                    .addHeader("Authorization", "Bearer $OPENAI_API_KEY")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("OpenAI-Beta", "assistants=v2")
                    .get()
                    .build()
                val msgRes = client.newCall(msgReq).execute()
                val msgBody = msgRes.body?.string()
                val msgs = JSONObject(msgBody ?: "{}").optJSONArray("data")
                for (i in 0 until (msgs?.length() ?: 0)) {
                    val msg = msgs!!.optJSONObject(i)
                    if (msg?.optString("role") == "assistant") {
                        val contentArr = msg.optJSONArray("content")
                        val textObj = contentArr?.optJSONObject(0)?.optJSONObject("text")
                        val value = textObj?.optString("value")
                        if (!value.isNullOrEmpty()) return@withContext value
                    }
                }
                return@withContext null
            }
        }
        null
    }

}
