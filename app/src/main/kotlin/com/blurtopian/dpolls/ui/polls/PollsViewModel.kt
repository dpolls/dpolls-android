package com.blurtopian.dpolls.ui.polls

import android.util.Log
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
        Log.d("PollsViewModel", "Initializing PollsViewModel")
        refreshPolls()
    }

    fun refreshPolls() {
        Log.d("PollsViewModel", "Starting to refresh polls")
        viewModelScope.launch {
            try {
                _uiState.value = PollsUiState.Loading
                Log.d("PollsViewModel", "Calling repository.getPolls()")
                val polls = repository.getPolls()
                Log.d("PollsViewModel", "Received ${polls.size} polls from repository")
                polls.forEachIndexed { index, poll ->
                    Log.d("PollsViewModel", "Poll $index: id=${poll.id}, title=${poll.title}, active=${poll.isActive}")
                }
                _uiState.value = PollsUiState.Success(polls)
                Log.d("PollsViewModel", "Successfully updated UI state with polls")
            } catch (e: Exception) {
                Log.e("PollsViewModel", "Error fetching polls", e)
                _uiState.value = PollsUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
    
    fun onPollSelected(poll: Poll) {
        Log.d("PollsViewModel", "Poll selected: ${poll.id}")
        // Navigate to poll details
    }
    
    fun onCreatePollClicked() {
        Log.d("PollsViewModel", "Create poll clicked")
        // Navigate to create poll screen
    }
}

sealed class PollsUiState {
    object Loading : PollsUiState()
    data class Success(val polls: List<Poll>) : PollsUiState()
    data class Error(val message: String) : PollsUiState()
} 