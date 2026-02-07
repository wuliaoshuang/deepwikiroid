package com.moxiang.deepwiki.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.moxiang.deepwiki.DeepWikiApplication
import com.moxiang.deepwiki.data.crawler.DeepWikiCrawler
import com.moxiang.deepwiki.data.model.DocumentDto
import com.moxiang.deepwiki.data.repository.FavoriteRepository
import com.moxiang.deepwiki.data.repository.HistoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Success(val document: DocumentDto) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}

class RepoDetailViewModel(
    private val favoriteRepository: FavoriteRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {
    private val crawler = DeepWikiCrawler()

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _repoName = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val isFavorite: StateFlow<Boolean> = _repoName
        .flatMapLatest { name -> favoriteRepository.isFavorite(name) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun fetchDocument(path: String) {
        _repoName.value = path
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            try {
                val document = crawler.fetchDocument(path)
                _uiState.value = DetailUiState.Success(document)
            } catch (e: Exception) {
                _uiState.value = DetailUiState.Error(e.message ?: "Failed to load document")
            }
        }
    }

    fun toggleFavorite(description: String, stars: Int) {
        viewModelScope.launch {
            val name = _repoName.value
            if (name.isNotBlank()) {
                val current = isFavorite.value
                if (current) {
                    favoriteRepository.removeFavorite(name)
                } else {
                    favoriteRepository.addFavorite(name, description, stars)
                }
            }
        }
    }

    fun recordHistory(repoName: String, description: String, stars: Int) {
        viewModelScope.launch {
            historyRepository.recordHistory(repoName, description, stars)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application = checkNotNull(extras[APPLICATION_KEY]) as DeepWikiApplication
                return RepoDetailViewModel(application.repository, application.historyRepository) as T
            }
        }
    }
}
