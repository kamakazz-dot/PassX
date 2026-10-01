package ru.kk.passx.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import ru.kk.passx.data.repository.PasswordRepository
import ru.kk.passx.domain.model.Category
import ru.kk.passx.domain.model.PasswordItem
import ru.kk.passx.domain.model.SortOrder

data class VaultUiState(
    val passwords: List<PasswordItem> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: Category? = null,
    val isOnlyFavorites: Boolean = false,
    val sortOrder: SortOrder = SortOrder.BY_TITLE_ASC,
    val isLoading: Boolean = false
)

class VaultViewModel(
    private val repository: PasswordRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory

    private val _isOnlyFavorites = MutableStateFlow(false)
    private val _sortOrder = MutableStateFlow(SortOrder.BY_TITLE_ASC)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<VaultUiState> = combine(
        _searchQuery,
        _selectedCategory,
        _isOnlyFavorites,
        _sortOrder
    ) { query, category, onlyFav, sort ->
        Tuple4(query, category, onlyFav, sort)
    }.flatMapLatest { tuple ->
        val query = tuple.a
        val category = tuple.b
        val onlyFav = tuple.c
        val sort = tuple.d

        repository.getAllPasswords().map { list ->
            var result = list
            if (query.isNotBlank()) {
                val q = query.trim().lowercase()
                result = result.filter { item ->
                    item.title.lowercase().contains(q) ||
                            item.login.lowercase().contains(q) ||
                            item.serviceUrl.lowercase().contains(q) ||
                            item.notes.lowercase().contains(q) ||
                            item.category.displayName.lowercase().contains(q)
                }
            }
            if (category != null) {
                result = result.filter { it.category == category }
            }
            if (onlyFav) {
                result = result.filter { it.isFavorite }
            }
            result = when (sort) {
                SortOrder.BY_TITLE_ASC -> result.sortedBy { it.title.lowercase() }
                SortOrder.BY_TITLE_DESC -> result.sortedByDescending { it.title.lowercase() }
                SortOrder.BY_DATE_DESC -> result.sortedByDescending { it.createdAt }
                SortOrder.BY_DATE_ASC -> result.sortedBy { it.createdAt }
            }
            VaultUiState(
                passwords = result,
                searchQuery = query,
                selectedCategory = category,
                isOnlyFavorites = onlyFav,
                sortOrder = sort,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VaultUiState(isLoading = true)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelect(category: Category?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun toggleFavoritesFilter() {
        _isOnlyFavorites.value = !_isOnlyFavorites.value
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun toggleFavoriteStatus(item: PasswordItem) {
        viewModelScope.launch {
            repository.savePassword(item.copy(isFavorite = !item.isFavorite))
        }
    }

    fun savePassword(item: PasswordItem, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.savePassword(item)
            onComplete()
        }
    }

    fun deletePassword(item: PasswordItem) {
        viewModelScope.launch {
            repository.deletePassword(item)
        }
    }

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    class Factory(private val repository: PasswordRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VaultViewModel(repository) as T
        }
    }
}
