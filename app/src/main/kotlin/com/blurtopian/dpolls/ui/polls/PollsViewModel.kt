package com.blurtopian.dpolls.ui.polls

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.blurtopian.dpolls.domain.model.Poll
import com.blurtopian.dpolls.domain.repository.PollsRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PollsViewModel @Inject constructor(
    private val repository: PollsRepositoryImpl
) : ViewModel() {
    
    val polls: List<Poll> = repository.getPolls()

    init {
        refreshPolls()
    }

    fun refreshPolls() {
        viewModelScope.launch {
            try {
                // The Flow will automatically emit new values
            } catch (e: Exception) {
                // Handle error
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