package com.moxiang.deepwiki.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moxiang.deepwiki.data.crawler.DeepWikiCrawler
import com.moxiang.deepwiki.data.model.RepositoryDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 首页 ViewModel
 * 负责调用 API 获取热门仓库并管理 UI 状态
 */
data class HomeUiState(
    val isLoading: Boolean = false,
    val trendingRepos: List<RepositoryDto> = emptyList(),
    val errorDetail: String? = null
)

class HomeViewModel : ViewModel() {
    private val crawler = DeepWikiCrawler()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadTrendingRepositories()
    }

    fun loadTrendingRepositories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorDetail = null) }
            try {
                // 调用我们之前写好的 fetchPopularRepositories (内部指向 Devin API)
                val repos = crawler.fetchPopularRepositories()
                _uiState.update { it.copy(isLoading = false, trendingRepos = repos) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorDetail = e.localizedMessage ?: ""
                    )
                }
            }
        }
    }
}
