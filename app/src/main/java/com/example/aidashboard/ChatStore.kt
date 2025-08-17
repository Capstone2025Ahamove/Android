package com.example.aidashboard

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "chat_store")

object ChatStore {
    private val KEY_SESSIONS = stringPreferencesKey("chat_sessions")
    private val gson = Gson()
    private val listType = object : TypeToken<List<ChatSession>>() {}.type

    private suspend fun loadAllInternal(context: Context): MutableList<ChatSession> {
        val prefs = context.dataStore.data.first()
        val json = prefs[KEY_SESSIONS] ?: "[]"
        return gson.fromJson<List<ChatSession>>(json, listType)?.toMutableList() ?: mutableListOf()
    }

    private suspend fun saveAll(context: Context, list: List<ChatSession>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SESSIONS] = gson.toJson(list)
        }
    }

    suspend fun getAll(context: Context): List<ChatSession> =
        loadAllInternal(context)

    suspend fun getById(context: Context, id: String): ChatSession? =
        loadAllInternal(context).firstOrNull { it.id == id }

    suspend fun upsert(context: Context, session: ChatSession) {
        val list = loadAllInternal(context)
        val idx = list.indexOfFirst { it.id == session.id }
        if (idx >= 0) list[idx] = session else list.add(session)
        saveAll(context, list)
    }

    suspend fun delete(context: Context, id: String) {
        val list = loadAllInternal(context)
        saveAll(context, list.filterNot { it.id == id })
    }

    suspend fun clear(context: Context) {
        saveAll(context, emptyList())
    }


    suspend fun getByThreadId(context: Context, threadId: String): ChatSession? =
        loadAllInternal(context).firstOrNull { it.threadId == threadId }

    suspend fun upsertByThreadId(context: Context, session: ChatSession): ChatSession {
        // Ensure session.id == session.threadId for stable upserts
        val fixed = if (session.threadId != null && session.id != session.threadId) {
            session.copy(id = session.threadId)
        } else session
        upsert(context, fixed)
        return fixed
    }

}
