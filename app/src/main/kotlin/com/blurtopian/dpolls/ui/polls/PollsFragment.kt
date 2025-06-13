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
            // Handle poll click
        }
        binding.pollsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@PollsFragment.adapter
        }
    }
    
    private fun observePolls() {
        adapter.submitList(viewModel.polls)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 