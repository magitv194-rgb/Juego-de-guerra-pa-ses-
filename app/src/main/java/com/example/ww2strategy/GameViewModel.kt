package com.example.ww2strategy

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ww2strategy.engine.GameEngine
import com.example.ww2strategy.model.*
import com.example.ww2strategy.storage.GameRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameRepository(app)
    private val _state = MutableStateFlow(
        repo.load()?.takeIf { saved ->
            saved.provinces.size == 24 && saved.provinces.any { it.id == "caba" }
        } ?: GameState()
    )
    val state: StateFlow<GameState> = _state.asStateFlow()
    private val _selectedProvince = MutableStateFlow<String?>(null)
    val selectedProvince: StateFlow<String?> = _selectedProvince.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                _state.update { GameEngine.tick(it) }
                delay(1_000)
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(10_000)
                repo.save(_state.value)
            }
        }
    }

    fun selectProvince(id: String) { _selectedProvince.value = id }

    fun produce(type: UnitType) {
        val pid = _selectedProvince.value ?: return
        GameEngine.startProduction(_state.value, pid, type, System.currentTimeMillis())
            ?.let { _state.value = it }
    }

    fun research(id: String) {
        GameEngine.startResearch(_state.value, id, System.currentTimeMillis())
            ?.let { _state.value = it }
    }

    fun construct(type: BuildingType) {
        val pid = _selectedProvince.value ?: return
        GameEngine.startConstruction(_state.value, pid, type, System.currentTimeMillis())
            ?.let { _state.value = it }
    }

    fun investMorale() {
        val pid = _selectedProvince.value ?: return
        GameEngine.investMorale(_state.value, pid)?.let { _state.value = it }
    }

    fun attack(fromId: String, toId: String) {
        GameEngine.launchAttack(_state.value, fromId, toId, System.currentTimeMillis())
            ?.let { _state.value = it }
    }

    fun nuke(targetId: String) {
        GameEngine.launchNuclear(_state.value, targetId, System.currentTimeMillis())
            ?.let { _state.value = it }
    }

    fun resetGame() {
        repo.clear()
        _state.value = GameState()
        _selectedProvince.value = null
    }
}
