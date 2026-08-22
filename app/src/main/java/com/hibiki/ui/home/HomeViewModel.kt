package com.hibiki.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hibiki.container
import com.hibiki.domain.model.AppPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val phraseCount: Int = 0,
    val audioCount: Int = 0,
    val hasApiKey: Boolean = false,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val container = application.container()

    val state: StateFlow<HomeUiState> = combine(
        container.phraseRepository.observeCount(),
        container.phraseRepository.observeAudioCount(),
        container.settingsRepository.preferences,
    ) { phrases, audio, prefs: AppPreferences ->
        HomeUiState(
            phraseCount = phrases,
            audioCount = audio,
            hasApiKey = prefs.api.hasApiKey,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
