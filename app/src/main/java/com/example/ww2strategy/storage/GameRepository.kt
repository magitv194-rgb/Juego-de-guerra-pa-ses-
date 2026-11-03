package com.example.ww2strategy.storage

import android.content.Context
import com.example.ww2strategy.model.GameState
import kotlinx.serialization.json.Json

class GameRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("ww2_game", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun save(state: GameState) {
        prefs.edit().putString(KEY_STATE, json.encodeToString(GameState.serializer(), state)).apply()
    }

    fun load(): GameState? = prefs.getString(KEY_STATE, null)?.let {
        runCatching { json.decodeFromString(GameState.serializer(), it) }.getOrNull()
    }

    fun clear() = prefs.edit().clear().apply()

    private companion object { const val KEY_STATE = "state" }
}
