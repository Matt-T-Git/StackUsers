package com.stackusers.ui.userdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stackusers.data.model.UserDetail
import com.stackusers.domain.usecase.GetUserDetailUseCase
import com.stackusers.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Detail screen ViewModel
 *
 * Fetches the full user profile (including top tags) on init
 * Exposes a single [uiState] StateFlow the UI observes to update its UI state
 */
@HiltViewModel
class UserDetailViewModel @Inject constructor(
    private val getUserDetailUseCase: GetUserDetailUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<UserDetail>>(UiState.Loading)
    val uiState: StateFlow<UiState<UserDetail>> = _uiState.asStateFlow()

    /**
     * Initiates loading for given user ID
     * Called from the screen once the nav argument is available
     */
    fun loadUserDetail(userId: Long) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            getUserDetailUseCase(userId)
                .onSuccess { detail ->
                    _uiState.value = UiState.Success(detail)
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(
                        error.message ?: "Failed to load user detail"
                    )
                }
        }
    }

    /**
     * Retries loading the user detail after an error
     * Exposed so the UI can offer a retry
     */
    fun retry(userId: Long) {
        loadUserDetail(userId)
    }
}
