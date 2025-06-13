package com.blurtopian.dpolls.ui.polls

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blurtopian.dpolls.data.model.Poll
import com.blurtopian.dpolls.data.repository.PollsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PollsViewModel @Inject constructor(
    private val pollsRepository: PollsRepository
) : ViewModel() {
    
    private val _uiState = MutableLiveData<PollsUiState>()
    val uiState: LiveData<PollsUiState> = _uiState
    
    init {
        loadPolls()
    }
    
    private fun loadPolls() {
        viewModelScope.launch {
            _uiState.value = PollsUiState.Loading
            try {
                val polls = pollsRepository.getPolls()
                _uiState.value = PollsUiState.Success(polls)
            } catch (e: Exception) {
                _uiState.value = PollsUiState.Error(e.message ?: "Failed to load polls")
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