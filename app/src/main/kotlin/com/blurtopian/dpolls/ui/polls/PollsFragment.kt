package com.blurtopian.dpolls.ui.polls

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.blurtopian.dpolls.databinding.FragmentPollsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PollsFragment : Fragment() {
    
    private var _binding: FragmentPollsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PollsViewModel by viewModels()
    private lateinit var pollsAdapter: PollsAdapter
    
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
        setupUI()
        observeViewModel()
    }
    
    private fun setupRecyclerView() {
        pollsAdapter = PollsAdapter { poll ->
            // Handle poll click
            viewModel.onPollSelected(poll)
        }
        binding.pollsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = pollsAdapter
        }
    }
    
    private fun setupUI() {
        binding.createPollFab.setOnClickListener {
            viewModel.onCreatePollClicked()
        }
    }
    
    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is PollsUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.pollsRecyclerView.visibility = View.GONE
                }
                is PollsUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.pollsRecyclerView.visibility = View.VISIBLE
                    pollsAdapter.submitList(state.polls)
                }
                is PollsUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    // Show error message
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 