package com.stackusers.ui.screens.userlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stackusers.data.model.User
import com.stackusers.domain.usecase.SearchUsersUseCase
import com.stackusers.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SEARCH_DEBOUNCE_MS = 800L
private const val MIN_QUERY_LENGTH = 2

/**
 * ViewModel for the User List screen
 *
 * Exposes:
 * - [searchQuery]: current text field value
 * - [uiState]: the current UI state (Idle, Loading, Success, Error, Empty)
 *
 * Search is debounced to avoid hammering the API on every keystroke
 * Manual submission via the search button bypasses the debounce but updates
 * [lastSearchedQuery] so the debounce pipeline skips the duplicate call
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class UserListViewModel @Inject constructor(
    private val searchUsersUseCase: SearchUsersUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<List<User>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<User>>> = _uiState.asStateFlow()

    // Tracks the last query that was actually sent to the API.
    // Prevents the debounce pipeline firing a duplicate call after
    // the user has already triggered a manual search via the button.
    private var lastSearchedQuery: String = ""

    init {
        observeSearchQuery()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    /**
     * Triggered by the search button. Executes immediately and records
     * the query so the debounce observer can skip it
     */
    fun onSearchSubmitted() {
        val query = _searchQuery.value
        if (query.length > MIN_QUERY_LENGTH) {
            lastSearchedQuery = query
            executeSearch(query)
        }
    }

    fun onSearchCleared() {
        _searchQuery.value = ""
        lastSearchedQuery = ""
        _uiState.value = UiState.Idle
    }

    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .filter { it.length > MIN_QUERY_LENGTH }
                .collectLatest { query ->
                    // Skip if already searched via the button
                    if (query != lastSearchedQuery) {
                        lastSearchedQuery = query
                        executeSearch(query)
                    }
                }
        }
    }

    private fun executeSearch(query: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            searchUsersUseCase(query)
                .onSuccess { users ->
                    _uiState.value = if (users.isEmpty()) {
                        UiState.Empty
                    } else {
                        UiState.Success(users)
                    }
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(
                        error.message ?: "An unexpected error occurred"
                    )
                }
        }
    }
}