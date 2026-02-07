package com.moxiang.deepwiki.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.moxiang.deepwiki.DeepWikiApplication
import com.moxiang.deepwiki.data.local.entity.FavoriteEntity
import com.moxiang.deepwiki.data.repository.FavoriteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {
    val favorites: StateFlow<List<FavoriteEntity>> = favoriteRepository.allFavorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addFavorite(repoName: String, description: String, stars: Int) {
        viewModelScope.launch {
            favoriteRepository.addFavorite(repoName, description, stars)
        }
    }

    fun removeFavorite(repoName: String) {
        viewModelScope.launch {
            favoriteRepository.removeFavorite(repoName)
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
                return FavoritesViewModel(application.repository) as T
            }
        }
    }
}
