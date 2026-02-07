package com.moxiang.deepwiki.feature.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.moxiang.deepwiki.data.crawler.DeepWikiCrawler
import com.moxiang.deepwiki.data.model.RepositoryDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<RepoUiModel> = emptyList(),
    val popularRepositories: List<RepoUiModel> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val errorDetail: String? = null
)

data class RepoUiModel(
    val fullName: String,
    val description: String,
    val stars: Int,
    val language: String?,
    val url: String
)

class SearchViewModel(
    private val historyStore: SearchHistoryStore
) : ViewModel() {
    private val crawler = DeepWikiCrawler()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadPopularRepositories()
        loadRecentSearches()
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }

        // Debounce search
        searchJob?.cancel()
        if (newQuery.isNotBlank()) {
            searchJob = viewModelScope.launch {
                delay(500) // 500ms debounce
                performSearch(newQuery)
            }
        } else {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
        }
    }

    fun onSearchTriggered() {
        val query = _uiState.value.query.trim()
        if (query.isNotBlank()) {
            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                performSearch(query)
            }
        }
    }

    fun onRecentSearchSelected(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        _uiState.update { it.copy(query = trimmed) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            performSearch(trimmed)
        }
    }

    fun clearRecentSearches() {
        historyStore.save(emptyList())
        _uiState.update { it.copy(recentSearches = emptyList()) }
    }

    fun removeRecentSearch(query: String) {
        _uiState.update { state ->
            val updated = state.recentSearches.filterNot { it.equals(query, true) }
            historyStore.save(updated)
            state.copy(recentSearches = updated)
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.update { it.copy(isSearching = true, errorDetail = null) }

        try {
            val results = crawler.searchRepositories(query)
            _uiState.update {
                it.copy(
                    isSearching = false,
                    searchResults = results.map { dto -> dto.toUiModel() }
                )
            }
            addRecentSearch(query)
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isSearching = false,
                    errorDetail = e.localizedMessage ?: ""
                )
            }
        }
    }

    private fun loadPopularRepositories() {
        viewModelScope.launch {
            try {
                val results = crawler.fetchPopularRepositories()
                if (results.isNotEmpty()) {
                    _uiState.update {
                        it.copy(popularRepositories = results.map { dto -> dto.toUiModel() })
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadRecentSearches() {
        val stored = historyStore.load()
        if (stored.isNotEmpty()) {
            _uiState.update { it.copy(recentSearches = stored) }
        }
    }

    private fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        _uiState.update { state ->
            val updated = listOf(trimmed) + state.recentSearches.filterNot { it.equals(trimmed, true) }
            val limited = updated.take(8)
            historyStore.save(limited)
            state.copy(recentSearches = limited)
        }
    }

    private fun RepositoryDto.toUiModel(): RepoUiModel {
        return RepoUiModel(
            fullName = this.fullName,
            description = this.description,
            stars = this.stars,
            language = this.language,
            url = this.url
        )
    }
}

class SearchViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            return SearchViewModel(SearchHistoryStore(context.applicationContext)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
