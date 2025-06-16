package com.blurtopian.dpolls.ui.polls

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.blurtopian.dpolls.databinding.FragmentPollsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PollsFragment : Fragment() {
    
    private var _binding: FragmentPollsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PollsViewModel by viewModels()
    private lateinit var adapter: PollsAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPollsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observePolls()
    }
    
    private fun setupRecyclerView() {
        adapter = PollsAdapter { poll ->
            viewModel.onPollSelected(poll)
        }
        binding.pollsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@PollsFragment.adapter
        }
    }
    
    private fun observePolls() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is PollsUiState.Loading -> {
                            // Show loading state
                            binding.progressBar.visibility = View.VISIBLE
                            binding.pollsRecyclerView.visibility = View.GONE
                            binding.errorText.visibility = View.GONE
                        }
                        is PollsUiState.Success -> {
                            // Show polls
                            binding.progressBar.visibility = View.GONE
                            binding.pollsRecyclerView.visibility = View.VISIBLE
                            binding.errorText.visibility = View.GONE
                            adapter.submitList(state.polls)
                        }
                        is PollsUiState.Error -> {
                            // Show error
                            binding.progressBar.visibility = View.GONE
                            binding.pollsRecyclerView.visibility = View.GONE
                            binding.errorText.visibility = View.VISIBLE
                            binding.errorText.text = state.message
                        }
                    }
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 