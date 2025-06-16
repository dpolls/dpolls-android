package com.blurtopian.dpolls.ui.polls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blurtopian.dpolls.domain.model.Poll
import com.blurtopian.dpolls.domain.repository.PollsRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PollsViewModel @Inject constructor(
    private val repository: PollsRepositoryImpl
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<PollsUiState>(PollsUiState.Loading)
    val uiState: StateFlow<PollsUiState> = _uiState.asStateFlow()

    init {
        refreshPolls()
    }

    fun refreshPolls() {
        viewModelScope.launch {
            try {
                _uiState.value = PollsUiState.Loading
                val polls = repository.getPolls()
                _uiState.value = PollsUiState.Success(polls)
            } catch (e: Exception) {
                _uiState.value = PollsUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
    
    fun onPollSelected(poll: Poll) {
        // Navigate to poll details
    }
    
    fun onCreatePollClicked() {
        // Navigate to create poll screen
    }
}

sealed class PollsUiState {
    object Loading : PollsUiState()
    data class Success(val polls: List<Poll>) : PollsUiState()
    data class Error(val message: String) : PollsUiState()
} 